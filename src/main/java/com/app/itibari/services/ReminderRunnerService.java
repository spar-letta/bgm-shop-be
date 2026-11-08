package com.app.itibari.services;

import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.*;
import com.app.itibari.repository.CreditLimitItemRepository;
import com.app.itibari.repository.ReminderJobRepository;
import com.app.itibari.repository.ReminderLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderRunnerService {


    // if reminder setting is null... we result to this
    private final static String DEFAULT_DUE_SOON_TEMPLATE = "Dear {{customerName}}, your invoice {{invoiceNumber}} is due on {{dueDate}}. Please make payment to avoid any inconvenience.";

    private final static String DEFAULT_OVERDUE_TEMPLATE = "Dear {{customerName}}, your invoice {{invoiceNumber}} was due on {{dueDate}} and is now {{daysOverdue}} days overdue. Please make payment immediately to avoid further action.";

    private final ReminderJobRepository reminderJobRepository;
    private final ReminderLogRepository reminderLogRepository;
    private final ItibariSmsService itibariSmsService;
    private final CreditLimitItemRepository creditLimitItemRepository;

    /**
     * Polls every 20 minutes
     */
    @Scheduled(fixedDelay = 20 * 60 * 1000)
    @Transactional
    public void processScheduledJobs() {
        List<ReminderJob> dueJobs = reminderJobRepository
                .findAllByStatusAndScheduledTimeLessThanEqualAndDeletedIsFalse(
                        ReminderJobStatus.SCHEDULED, LocalDateTime.now());

        log.info("====Reminder jobs found: {}", dueJobs.size());

        for (ReminderJob job : dueJobs) {
            executeJob(job);
        }
    }

    private void executeJob(ReminderJob job) {
        log.info("Reminder job found: {}", job.getJobType());
        job.setStatus(ReminderJobStatus.RUNNING);
        reminderJobRepository.save(job);

        try {
            List<CreditLimitItem> items = fetchCreditLimitItems(job.getJobType(), job.getReminderSetting().getPeriod());

            for (CreditLimitItem item : items) {
                if (isAlreadySent(job, item)) {
                    continue;
                }

                Wallet wallet = item.getWallet();
                if (wallet == null) continue;

                Customers customer = wallet.getCustomer();
                if (customer == null) continue;

                if (!matchesCustomerType(job, customer)) continue;

                boolean sent = dispatchNotification(job, item, customer);
                logResult(job, item, customer, sent);
            }

            job.setStatus(ReminderJobStatus.COMPLETED);
            scheduleNextOccurrence(job);

        } catch (Exception e) {
            log.error("Reminder job {} failed: {}", job.getPublicId(), e.getMessage(), e);
            job.setStatus(ReminderJobStatus.FAILED);
            job.setFailureReason(e.getMessage());
        }

        reminderJobRepository.save(job);
    }

    private List<CreditLimitItem> fetchCreditLimitItems(ReminderJobType jobType, Integer period) {
        LocalDate today = LocalDate.now();
        return switch (jobType) {
            case DUE_SOON -> creditLimitItemRepository.findCreditOrdersDueSoon(today.plusDays(period));
            case OVERDUE -> creditLimitItemRepository.findOverdueCreditOrders(today.minusDays(period));
        };
    }

    private boolean isAlreadySent(ReminderJob job, CreditLimitItem item) {
        return reminderLogRepository.existsByReminderJobAndOrderNumber(job, item.getOrderNumber());
    }

    private boolean matchesCustomerType(ReminderJob job, Customers customer) {
        ReminderSetting setting = job.getReminderSetting();
        if (setting == null) return true;

        CustomerType required = setting.getCustomerType();
        if (required == null) return true; // null means all customer types
        CustomerType actual = customer.getCustomerType();
        return actual != null && actual == required;
    }

    private boolean dispatchNotification(ReminderJob job, CreditLimitItem item, Customers customer) {
        String message = buildMessage(job, item, customer);

        NotificationChannel channel = job.getNotificationChannel();

        try {
            if (channel == NotificationChannel.SMS) {
                String phone = customer.getPhoneNumber();
                if (phone != null && !phone.isBlank()) {
                    log.info("=========Sending SMS notification to {} with message {} ===========", phone, message);
                    itibariSmsService.send(phone, message);
                } else {
                    log.warn("Customer {} has no phone number; skipping SMS", customer.getPublicId());
                    return false;
                }
            }
            // PUSH channel — placeholder for future push notification integration
            return true;
        } catch (Exception e) {
            log.error("Failed to send notification to customer {}: {}", customer.getPublicId(), e.getMessage());
            return false;
        }
    }

    private String buildMessage(ReminderJob job, CreditLimitItem item, Customers customer) {

        ReminderSetting setting = job.getReminderSetting();
        String template = (setting != null)
                ? setting.getMessageTemplate()
                : (job.getJobType() == ReminderJobType.DUE_SOON ? DEFAULT_DUE_SOON_TEMPLATE : DEFAULT_OVERDUE_TEMPLATE);

        LocalDate dueDate = item.getDueDate();
        long daysOverdue = (dueDate != null) ? LocalDate.now().toEpochDay() - dueDate.toEpochDay() : 0;

        template = TemplateVariable.CUSTOMER_NAME.resolve(template, customer.getName());
        template = TemplateVariable.INVOICE_NUMBER.resolve(template, item.getOrderNumber());
        template = TemplateVariable.DUE_DATE.resolve(template, dueDate != null ? dueDate.toString() : "");
        template = TemplateVariable.DAYS_OVERDUE.resolve(template, String.valueOf(Math.max(daysOverdue, 0)));
        template = TemplateVariable.AMOUNT_DUE.resolve(template,
                item.getBalanceAmount() != null ? item.getBalanceAmount().toPlainString() : "0");
        template = TemplateVariable.COMPANY_NAME.resolve(template, "Itibari");

        return template;
    }

    private void logResult(ReminderJob job, CreditLimitItem item, Customers customer, boolean sent) {
        ReminderLog reminderLog = new ReminderLog();
        reminderLog.setReminderJob(job);
        reminderLog.setOrderNumber(item.getOrderNumber());
        reminderLog.setCustomer(customer);
        reminderLog.setSent(sent);
        reminderLog.setSentAt(LocalDateTime.now());
        reminderLog.setMessage(buildMessage(job, item, customer));
        reminderLogRepository.save(reminderLog);
    }

    /**
     * After a COMPLETED job, schedule the next occurrence if the linked
     * ReminderSetting has a recurring frequency.
     */
    private void scheduleNextOccurrence(ReminderJob completedJob) {
        ReminderSetting setting = completedJob.getReminderSetting();
        if (setting == null || setting.getFrequency() == ReminderFrequency.ONCE) return;

        LocalDateTime next = switch (setting.getFrequency()) {
            case DAILY -> completedJob.getScheduledTime().plusDays(1);
            case AFTER_EVERY_TWO_DAYS -> completedJob.getScheduledTime().plusDays(2);
            case WEEKLY -> completedJob.getScheduledTime().plusWeeks(1);
            default -> null;
        };

        if (next == null) return;

        ReminderJob nextJob = new ReminderJob();
        nextJob.setJobType(completedJob.getJobType());
        nextJob.setScheduledTime(next);
        nextJob.setNotificationChannel(completedJob.getNotificationChannel());
        nextJob.setStatus(ReminderJobStatus.SCHEDULED);
        nextJob.setReminderSetting(setting);
        nextJob.setCreatedBy(completedJob.getCreatedBy());
        reminderJobRepository.save(nextJob);
    }
}

package com.app.itibari.configuration;

import com.app.itibari.dataTypes.IdentificationType;
import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.request.DeliveryAgentsRequest;
import com.app.itibari.models.request.ProductProcess;
import com.app.itibari.models.request.SkuProcess;
import com.app.itibari.models.request.SupplierRequest;
import com.app.itibari.repository.CustomersRepository;
import com.app.itibari.utils.batch.*;
import com.app.itibari.utils.specification.CustomerSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final CustomersRepository customersRepository;

    public BatchConfiguration(JobRepository jobRepository, PlatformTransactionManager transactionManager, CustomersRepository customersRepository) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.customersRepository = customersRepository;
    }


    @Bean
    public FlatFileItemReader<SupplierRequest> supplierRequestFlatFileItemReader() {
        FlatFileItemReader<SupplierRequest> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource("supplier.csv"));
        reader.setLinesToSkip(1);

        DefaultLineMapper<SupplierRequest> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setNames("Supplier Name", "Postal Address", "Physical Address", "Contact Person", "Phone Number", "Supplier Email", "TIN Number", "VRN", "Lead Days", "Credit Terms", "Credit Limit", "Bank Name", "Account Name", "Account Number");
        lineTokenizer.setDelimiter(":");

        FieldSetMapper<SupplierRequest> fieldSetMapper = fieldSet -> {
            SupplierRequest dto = new SupplierRequest();
            dto.setSupplierName(fieldSet.readString("Supplier Name"));
            dto.setPostalAddress(fieldSet.readString("Postal Address"));
            dto.setSupplierAddress(fieldSet.readString("Physical Address"));
            dto.setSupplierPointOfContact(fieldSet.readString("Contact Person"));
            dto.setPhoneNumber(fieldSet.readString("Phone Number"));
            dto.setEmail(fieldSet.readString("Supplier Email"));
            dto.setTinNumber(fieldSet.readString("TIN Number"));
            dto.setLeadDays(fieldSet.readInt("Lead Days"));
            dto.setCreditTerms(fieldSet.readInt("Credit Terms"));
            dto.setCreditLimit(fieldSet.readBigDecimal("Credit Limit"));
            dto.setBankName(fieldSet.readString("Bank Name"));
            dto.setBankAccountName(fieldSet.readString("Account Name"));
            dto.setBankAccountNumber(fieldSet.readString("Account Number"));
            return dto;
        };

        lineMapper.setLineTokenizer(lineTokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        reader.setLineMapper(lineMapper);

        return reader;
    }

    @Bean
    public SupplierProcessor supplierProcessor() {
        return new SupplierProcessor();
    }

    @Bean
    public SupplierWriter supplierWriter() {
        return new SupplierWriter();
    }

    @Bean(name = "ImportSupplierJob")
    public Job supplierJob(SupplierProcessor processor, SupplierWriter writer) {
        Step step = new StepBuilder("supplier-step", jobRepository)
                .<SupplierRequest, SupplierRequest>chunk(10, transactionManager)
                .reader(supplierRequestFlatFileItemReader())
                .processor(processor)
                .writer(writer)
                .build();

        return new JobBuilder("ETL-Supplier-Load", jobRepository)
                .start(step)
                .build();
    }


    /**
     * Delivery agents reader configuration
     */

    @Bean
    public FlatFileItemReader<DeliveryAgentsRequest> deliveryAgentsReader() {
        FlatFileItemReader<DeliveryAgentsRequest> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource("delivery_agents.csv"));
        reader.setLinesToSkip(1);

        DefaultLineMapper<DeliveryAgentsRequest> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setNames("Full Name", "Identification Type", "Identification Number", "Residential Address", "Phone Number");
        lineTokenizer.setDelimiter(":");

        FieldSetMapper<DeliveryAgentsRequest> fieldSetMapper = fieldSet -> {
            DeliveryAgentsRequest dto = new DeliveryAgentsRequest();
            dto.setFirstName(fieldSet.readString("Full Name"));
            dto.setIdentificationType(IdentificationType.valueOf(fieldSet.readString("Identification Type")));
            dto.setIdentificationNumber(fieldSet.readString("Identification Number"));
            dto.setResidentialAddress(fieldSet.readString("Residential Address"));
            dto.setPhoneNumber(fieldSet.readString("Phone Number"));
            dto.setLastName("");

            return dto;
        };

        lineMapper.setLineTokenizer(lineTokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        reader.setLineMapper(lineMapper);

        return reader;
    }


    @Bean
    public DeliveryAgentProcessor deliveryAgentsProcessor() {
        return new DeliveryAgentProcessor();
    }

    @Bean
    public DeliveryAgentWriter deliveryAgentsWriter() {
        return new DeliveryAgentWriter();
    }

    @Bean(name = "ImportDeliveryAgentsJob")
    public Job deliveryAgentsJob(DeliveryAgentProcessor deliveryAgentProcessor, DeliveryAgentWriter deliveryAgentWriter) {
        Step step = new StepBuilder("delivery-agents-step", jobRepository)
                .<DeliveryAgentsRequest, DeliveryAgentsRequest>chunk(10, transactionManager)
                .reader(deliveryAgentsReader())
                .processor(deliveryAgentProcessor)
                .writer(deliveryAgentWriter)
                .build();

        return new JobBuilder("ETL-Delivery-Agents-Load", jobRepository)
                .start(step)
                .build();
    }

    /**
     * Products reader configuration
     */

    @Bean
    public FlatFileItemReader<ProductProcess> productsReader() {
        FlatFileItemReader<ProductProcess> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource("products.csv"));
        reader.setLinesToSkip(1);

        DefaultLineMapper<ProductProcess> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setNames("Product Name", "Buying Price", "Selling Price", "Tax Mapping", "Specify Custom Tax", "Supplier", "Category", "Class", "Sub Class", "Purchase Unit Size", "Purchase Unit Name", "Selling Unit", "Unit of Measurement", "Bar Code", "Weight", "Length", "Width", "Height");
        lineTokenizer.setDelimiter(":");

        FieldSetMapper<ProductProcess> fieldSetMapper = fieldSet -> {
            ProductProcess dto = new ProductProcess();
            dto.setProductName(fieldSet.readString("Product Name"));
            dto.setBuyingPrice(fieldSet.readString("Buying Price"));
            dto.setSellingPrice(fieldSet.readString("Selling Price"));
            dto.setTaxMapping(fieldSet.readString("Tax Mapping"));
            dto.setSpecifyCustomTax(fieldSet.readString("Specify Custom Tax"));
            dto.setCategory(fieldSet.readString("Category"));
            dto.setProductClass(fieldSet.readString("Class"));
            dto.setSubClass(fieldSet.readString("Sub Class"));
            dto.setPurchaseUnitSize(fieldSet.readString("Purchase Unit Size"));
            dto.setPurchaseUnitName(fieldSet.readString("Purchase Unit Name"));
            dto.setSellingUnit(fieldSet.readString("Selling Unit"));
            dto.setUnitOfMeasurement(fieldSet.readString("Unit of Measurement"));
            dto.setBarCode(fieldSet.readString("Bar Code"));
            dto.setWeight(fieldSet.readString("Weight"));
            dto.setLength(fieldSet.readString("Length"));
            dto.setWidth(fieldSet.readString("Width"));
            dto.setHeight(fieldSet.readString("Height"));

            // Handle the supplier field
            List<String> suppliers = List.of(fieldSet.readString("Supplier").split(","));
            dto.setSuppliers(suppliers);

            return dto;
        };

        lineMapper.setLineTokenizer(lineTokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        reader.setLineMapper(lineMapper);

        return reader;
    }

    @Bean
    public ProductProcessor productProcessor() {
        return new ProductProcessor();
    }

    @Bean
    public ProductWriter productWriter() {
        return new ProductWriter();
    }

    @Bean(name = "ImportProductsJob")
    public Job productJob(ProductProcessor processor, ProductWriter writer) {
        Step step = new StepBuilder("product-step", jobRepository)
                .<ProductProcess, ProductProcess>chunk(10, transactionManager)
                .reader(productsReader())
                .processor(processor)
                .writer(writer)
                .build();

        return new JobBuilder("ETL-Product-Load", jobRepository)
                .start(step)
                .build();
    }


    @Bean
    public FlatFileItemReader<SkuProcess> skuReader() {
        FlatFileItemReader<SkuProcess> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource("sku.csv"));
        reader.setLinesToSkip(1);

        DefaultLineMapper<SkuProcess> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        //Product,Sku,Qty,Price
        lineTokenizer.setNames("Product", "Sku", "Qty", "Price");
        lineTokenizer.setDelimiter(",");

        FieldSetMapper<SkuProcess> fieldSetMapper = fieldSet -> {
            SkuProcess dto = new SkuProcess();
            dto.setProductName(fieldSet.readString("Product"));
            dto.setQuantity(fieldSet.readInt("Qty"));
            dto.setSkuCode(fieldSet.readString("Sku"));
            dto.setPrice(fieldSet.readString("Price"));
            return dto;
        };

        lineMapper.setLineTokenizer(lineTokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        reader.setLineMapper(lineMapper);
        return reader;
    }

    @Bean
    public SkuProcessor skuProcessor() {
        return new SkuProcessor();
    }

    @Bean
    public SkuProcessWriter skuWriter() {
        return new SkuProcessWriter();
    }

    @Bean(name = "ImportSkuJob")
    public Job skuJob(SkuProcessor skuProcessor, SkuProcessWriter skuWriter) {
        Step step = new StepBuilder("sku-step", jobRepository)
                .<SkuProcess, SkuProcess>chunk(10, transactionManager)
                .reader(skuReader())
                .processor(skuProcessor)
                .writer(skuWriter)
                .build();

        return new JobBuilder("ETL-Sku-Load", jobRepository)
                .start(step)
                .build();
    }

//=========================tuma msg=================

    @Bean
    @StepScope
    public ItemReader<Customers> activeCustomerJobItemReader(@Value("#{jobParameters['message']}") String message,
                                                             @Value("#{jobParameters['variables']}") String variables,
                                                             @Value("#{jobParameters['campaignName']}") String campaignName
    ) {

        RepositoryItemReader<Customers> reader = new RepositoryItemReader<>();
        reader.setRepository(customersRepository);
        reader.setMethodName("findByDeletedIsFalseAndActiveIsTrue");

        Map<String, Sort.Direction> sort = new HashMap<>();
        sort.put("id", Sort.Direction.ASC);

        reader.setSort(sort);
        return reader;
    }

    @Bean
    public ActiveCustomerScheduleJobItemProcessor activeCustomerScheduleJobItemProcessor() {
        return new ActiveCustomerScheduleJobItemProcessor();
    }

    @Bean
    public ActiveCustomerScheduleJobItemWriter activeCustomerScheduleJobItemWriter() {
        return new ActiveCustomerScheduleJobItemWriter();
    }

    @Bean(name = "ActiveCustomerScheduleJob")
    public Job activeCustomersJob(ActiveCustomerScheduleJobItemProcessor activeCustomerScheduleJobItemProcessor, ActiveCustomerScheduleJobItemWriter activeCustomerScheduleJobItemWriter) {
        Step step = new StepBuilder("active-customer-step", jobRepository)
                .<Customers, Customers>chunk(10, transactionManager)
                .reader(activeCustomerJobItemReader(null, null, null))
                .processor(activeCustomerScheduleJobItemProcessor)
                .writer(activeCustomerScheduleJobItemWriter)
                .build();

        return new JobBuilder("ETL-Active-Customers-Load", jobRepository)
                .start(step)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<Customers> bnplEligibleCustomerJobItemReader() {
        LocalDateTime periodEnd = LocalDateTime.now(ZoneId.of("Africa/Nairobi"));

        Specification<Customers> eligibleCustomers = Specification
                .<Customers>where((root, query, cb) -> cb.isFalse(root.get("deleted")))
                .and(CustomerSpecification.hasNotOnboardedCustomerWithThreeOrdersInEachRecentThirtyDayPeriod(periodEnd));

        RepositoryItemReader<Customers> reader = new RepositoryItemReader<>();
        reader.setRepository(customersRepository);
        reader.setMethodName("findAll");
        reader.setArguments(List.of(eligibleCustomers));

        Map<String, Sort.Direction> sort = new HashMap<>();
        sort.put("id", Sort.Direction.ASC);

        reader.setSort(sort);
        return reader;
    }

    @Bean(name = "BnplEligibleCustomerScheduleJob")
    public Job bnplEligibleCustomersJob(ActiveCustomerScheduleJobItemProcessor activeCustomerScheduleJobItemProcessor, ActiveCustomerScheduleJobItemWriter activeCustomerScheduleJobItemWriter) {
        Step step = new StepBuilder("bnpl-eligible-customer-step", jobRepository)
                .<Customers, Customers>chunk(10, transactionManager)
                .reader(bnplEligibleCustomerJobItemReader())
                .processor(activeCustomerScheduleJobItemProcessor)
                .writer(activeCustomerScheduleJobItemWriter)
                .build();

        return new JobBuilder("ETL-Bnpl-Eligible-Customers-Load", jobRepository)
                .start(step)
                .build();
    }

}

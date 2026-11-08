//package com.app.itibari.runner;
//
//import com.app.itibari.models.entities.GoodsReceivedNote;
//import com.app.itibari.repository.GoodsReceivedNoteRepository;
//import jakarta.transaction.Transactional;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Slf4j
//@Component
//public class StaticStockRunner implements CommandLineRunner {
//
//    private final GoodsReceivedNoteRepository goodsReceivedNoteRepository;
//
//    public StaticStockRunner(GoodsReceivedNoteRepository goodsReceivedNoteRepository) {
//        this.goodsReceivedNoteRepository = goodsReceivedNoteRepository;
//    }
//
//    @Override
//    @Transactional
//    public void run(String... args) {
//        log.info("---------goodsReceivedNote run started");
//
//        List<GoodsReceivedNote> notesToUpdate =
//                goodsReceivedNoteRepository.findByDeletedIsFalseAndCreationTimeIsNullAndNoteDateIsNotNull();
//
//        notesToUpdate.forEach(note -> note.setCreationTime(note.getNoteDate().atStartOfDay()));
//
//        if (!notesToUpdate.isEmpty()) {
//            goodsReceivedNoteRepository.saveAll(notesToUpdate);
//            log.info("-----Updated {} GoodsReceivedNotes with creationTime", notesToUpdate.size());
//        } else {
//            log.info("--------No GoodsReceivedNotes required updating");
//        }
//    }
//}
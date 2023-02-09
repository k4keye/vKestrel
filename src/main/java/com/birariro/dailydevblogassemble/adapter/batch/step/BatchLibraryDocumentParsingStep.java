package com.birariro.dailydevblogassemble.adapter.batch.step;

import com.birariro.dailydevblogassemble.adapter.parser.RSSParser;
import com.birariro.dailydevblogassemble.domain.library.Document;
import com.birariro.dailydevblogassemble.domain.library.Library;
import com.birariro.dailydevblogassemble.domain.library.LibraryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 하루에 한번
 * 각 사이트에서 게시글을 파싱한다.
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class BatchLibraryDocumentParsingStep {
    private final StepBuilderFactory stepBuilderFactory;
    private final LibraryRepository libraryRepository;
    private final RSSParser rssParser;
    private final CustomStepExecutionListener customStepExecutionListener;

    @Bean
    public Step libraryDocumentParsingStep(){
        return stepBuilderFactory.get("libraryDocumentParsingStep")
                .listener(customStepExecutionListener)
                .<Library, Library> chunk(10)
                .reader(libraryDocumentParsingReader())
                .processor(libraryDocumentParsingProcessor())
                .writer(libraryDocumentParsingWriter())
                .build();
    }


    @Bean
    @StepScope
    public ListItemReader<Library> libraryDocumentParsingReader(){
        List<Library> libraries = libraryRepository.findActiveByAll();
        return new ListItemReader<>(libraries);
    }


    public ItemProcessor<Library,Library> libraryDocumentParsingProcessor(){
        return item ->{

            log.info("RSS parser target name : " +item.getName());
            List<Document> documents = rssParser.getDocument(item.getUrl());

            List<Document> newDocuments = documents.stream()
                                                    .filter(target -> item.existDocument(target) == false)
                                                    .collect(Collectors.toList());


            newDocuments.forEach(document -> {
                log.info("[add document] " + item.getName() + ": "+document.getTitle());
                item.addDocument(document);
            });
            return item;
        };
    }

    public ItemWriter<Library> libraryDocumentParsingWriter(){
        return items -> {
            libraryRepository.saveAll(items);
        };
    }
}

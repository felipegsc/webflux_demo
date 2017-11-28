package com.example.webfluxdemo.controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.validation.Valid;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.webfluxdemo.model.Tweet;
import com.example.webfluxdemo.repository.TweetRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.FluxSink.OverflowStrategy;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/tweets")
public class TweetController {
    
    private static final int PERSISTERS = 10;
    
    private static Logger logger = LoggerFactory.getLogger(TweetController.class);
    
    private ExecutorService executorService = Executors.newFixedThreadPool(PERSISTERS); 
    
    @Autowired
    private TweetRepository tweetRepository;
    
    @GetMapping
    public Flux<Tweet> list() {
        return tweetRepository.findAll();
    }
    
    @PostMapping
    public Mono<Tweet> create(@Valid @RequestBody Publisher<Tweet> publisher) {
        return tweetRepository
                    .saveAll(publisher)
                    .next();
    }
    
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Tweet>> get(@PathVariable("id") String id) {
        return tweetRepository.findById(id)
                .map(tweet -> ResponseEntity.ok(tweet))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Tweet>> update(@PathVariable("id") String id, @Valid @RequestBody Tweet tweet) {
        return tweetRepository.findById(id)
                .flatMap(existingTweet -> {
                    existingTweet.setText(tweet.getText());
                    return tweetRepository.save(existingTweet);
                })
                .map(updatedTweet -> new ResponseEntity<>(updatedTweet, HttpStatus.OK))
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable("id") String id) {
        return tweetRepository.findById(id)
                .flatMap(existingTweet ->
                        tweetRepository.delete(existingTweet)
                            .then(Mono.just(new ResponseEntity<Void>(HttpStatus.OK)))
                            )
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @DeleteMapping("/all")
    public Mono<ResponseEntity<Void>> deleteAll() {
        return tweetRepository.deleteAll()
            .map(mono -> ResponseEntity.ok().build());
    }
    
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Tweet> streamAllTweets() {
        return tweetRepository.findAll();
    }
    
    @PostMapping("/populate")
    public void populate(@RequestParam(name = "count", defaultValue = "10000") final int count) {
        
        logger.info("Deleting current tweets...");
        tweetRepository
            .deleteAll()
            .block();
        logger.info("All tweets were deleted");
        
        
        Flux.create((FluxSink<Tweet> emitter) -> {
            
            AtomicInteger counter = new AtomicInteger();
            for (int i = 0; i < PERSISTERS; i++) {
                executorService.execute(new Persister(emitter, counter, count));
            }
            
            while (true) {
                int current = counter.get();
                if (current >= count) {
                    break;
                }
                try {
                    Thread.sleep(500l);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            
            emitter.complete();
            
        }, OverflowStrategy.BUFFER)
        .buffer(10_000)
        .subscribe(tweets -> {
            logger.info("Saving {} tweet(s)...", tweets.size());
            tweetRepository
                .saveAll(tweets)
                .blockLast();
            logger.info("Finished saving {} tweet(s)", tweets.size());
        });
        
    }
    
    private class Persister implements Runnable {
        
        private FluxSink<Tweet> emitter;
        private AtomicInteger counter;
        private int count;
        
        public Persister(FluxSink<Tweet> emitter, AtomicInteger counter, int count) {
            this.emitter = emitter;
            this.counter = counter;
            this.count = count;
        }

        @Override
        public void run() {
            int current;
            while ((current = this.counter.incrementAndGet()) <= count) {
                emitter.next(new Tweet(String.valueOf(current + 1)));
            }
        }
        
    }
    
}

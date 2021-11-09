package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

public final class MyRecursiveAction extends RecursiveAction {
    Clock clock;
    final String url;
    final Instant deadline;
    Map<String, Integer> counts;
    Set<String> visitedUrls;
    PageParserFactory parserFactory;
    List<Pattern> ignoredUrls;
    int maxDepth;

    @Inject
    public MyRecursiveAction (int maxDepth, String url, Instant deadline, Map<String, Integer> counts, Set<String> visitedUrls, Clock clock, List<Pattern> ignoredUrls, PageParserFactory parserFactory) {
        this.clock = clock;
        this.url = url;
        this.maxDepth = maxDepth;
        this.deadline = deadline;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.ignoredUrls = ignoredUrls;
        this.parserFactory = parserFactory;
    }

    @Override
    public void compute() {
        if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
            return;
        }
        if (visitedUrls.contains(url)) {
            return;
        }
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }
        if (!visitedUrls.add(url)) {
            return;
        }
        PageParser.Result result = parserFactory.get(url).parse();
        for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            if (counts.containsKey(e.getKey())) {
                counts.compute(e.getKey(), (k, v) -> e.getValue() + counts.get(e.getKey()) );
            } else {
                counts.compute(e.getKey(), (k ,v) -> e.getValue());
            }
        }
        List<MyRecursiveAction> mra = new ArrayList<>();
        for (String link : result.getLinks()) {
            MyRecursiveAction.Builder builder = new MyRecursiveAction.Builder()
                    .setClock(clock)
                    .setCounts(counts)
                    .setDeadline(deadline)
                    .setUrl(link)
                    .setVisitedUrls(visitedUrls)
                    .setMaxDepth(maxDepth - 1)
                    .setIgnoredUrls(ignoredUrls)
                    .setParserFactory(parserFactory);
            mra.add(builder.build());
        }
        invokeAll(mra);
    }

    public static class Builder {
        private int maxDepth=0;
        private String url="";
        private Instant deadline=null;
        private Map<String, Integer> counts=null;
        private Set<String> visitedUrls=null;
        private Clock clock=null;
        private List<Pattern> ignoredUrls=null;
        private PageParserFactory parserFactory = null;


        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setDeadline(Instant deadline) {
            this.deadline = deadline;
            return this;
        }

        public Builder setCounts(Map<String, Integer> counts) {
            this.counts = counts;
            return this;
        }

        public Builder setVisitedUrls(Set<String> visitedUrls) {
            this.visitedUrls = visitedUrls;
            return this;
        }

        public Builder setClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        public Builder setIgnoredUrls(List<Pattern> ignoredUrls) {
            this.ignoredUrls = ignoredUrls;
            return this;
        }

        public Builder setParserFactory(PageParserFactory parserFactory) {
            this.parserFactory = parserFactory;
            return this;
        }

        public MyRecursiveAction build(){
            return new MyRecursiveAction(maxDepth, url, deadline, counts, visitedUrls, clock, ignoredUrls, parserFactory);
        }
    }
}

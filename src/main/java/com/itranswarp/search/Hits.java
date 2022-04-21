package com.itranswarp.search;

import java.util.List;

public class Hits {

    public final int total;

    public final List<SearchableDocument> documents;

    public Hits(int total, List<SearchableDocument> documents) {
        this.total = total;
        this.documents = documents;
    }

    public static Hits empty() {
        return EMPTY;
    }

    private static final Hits EMPTY = new Hits(0, List.of());
}

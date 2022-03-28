package com.itranswarp.search;

import java.util.List;

public class Hits {

    public final int total;

    public final int offset;

    public final List<SearchableDocument> documents;

    public Hits(int total, int offset, List<SearchableDocument> documents) {
        this.total = total;
        this.offset = offset;
        this.documents = documents;
    }
}

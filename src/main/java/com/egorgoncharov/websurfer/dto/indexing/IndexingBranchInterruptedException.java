package com.egorgoncharov.websurfer.dto.indexing;

public class IndexingBranchInterruptedException extends RuntimeException {
    public IndexingBranchInterruptedException(String message) {
        super(message);
    }
}

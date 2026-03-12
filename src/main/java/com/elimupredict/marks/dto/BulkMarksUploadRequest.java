package com.elimupredict.marks.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkMarksUploadRequest {

    @NotEmpty
    private List<MarksUploadRequest> records;
}

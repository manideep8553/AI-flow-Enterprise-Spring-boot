package com.aiflow.enterprise.mapper;

import com.aiflow.enterprise.dto.response.DocumentResponse;
import com.aiflow.enterprise.entity.Document;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    DocumentResponse toResponse(Document document);

    List<DocumentResponse> toResponseList(List<Document> documents);
}

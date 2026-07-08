package com.aiflow.enterprise.mapper;

import com.aiflow.enterprise.dto.request.RequestRequest;
import com.aiflow.enterprise.dto.response.RequestResponse;
import com.aiflow.enterprise.entity.Request;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    Request toEntity(RequestRequest request);

    RequestResponse toResponse(Request request);

    List<RequestResponse> toResponseList(List<Request> requests);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(RequestRequest request, @MappingTarget Request entity);
}

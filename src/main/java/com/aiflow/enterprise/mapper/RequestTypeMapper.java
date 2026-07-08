package com.aiflow.enterprise.mapper;

import com.aiflow.enterprise.dto.request.RequestTypeRequest;
import com.aiflow.enterprise.dto.response.RequestTypeResponse;
import com.aiflow.enterprise.entity.RequestType;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RequestTypeMapper {

    RequestType toEntity(RequestTypeRequest request);

    RequestTypeResponse toResponse(RequestType requestType);

    List<RequestTypeResponse> toResponseList(List<RequestType> requestTypes);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(RequestTypeRequest request, @MappingTarget RequestType entity);
}

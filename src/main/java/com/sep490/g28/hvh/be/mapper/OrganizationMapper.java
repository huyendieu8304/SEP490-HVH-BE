package com.sep490.g28.hvh.be.mapper;

import com.sep490.g28.hvh.be.constant.EOrgType;
import com.sep490.g28.hvh.be.dto.organization.response.OrganizationSimpleResponse;
import com.sep490.g28.hvh.be.dto.organization.response.OrganizationSimpleResponseForSystemAdmin;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrganizationMapper {

    public OrganizationSimpleResponse toOrganizationSimpleResponse(Object[] row){
        return new OrganizationSimpleResponse(
                (UUID) row[0],
                (String) row[1],
                row[2] != null ? EOrgType.valueOf((String) row[2]) : null,
                Long.parseLong(row[3].toString()),
                Long.parseLong(row[4].toString()),
                Short.parseShort(row[5].toString())
        );
    }
}

package com.marakicode.financetracker.transactions;

import com.marakicode.financetracker.transactions.dto.TransactionCreateRequest;
import com.marakicode.financetracker.transactions.dto.TransactionResponse;
import com.marakicode.financetracker.transactions.dto.TransactionUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Transaction toEntity(TransactionCreateRequest request);

    @Mapping(source = "account.id", target = "accountId")
    @Mapping(source = "account.name", target = "accountName")
    @Mapping(source = "type", target = "type", qualifiedByName = "toEnum")
    @Mapping(source = "category", target = "category", qualifiedByName = "toCategoryName")
    TransactionResponse toResponse(Transaction transaction);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(TransactionUpdateRequest request, @MappingTarget Transaction transaction);

    @Named("toEnum")
    default TransactionType toEnum(TransactionTypeEntity entity) {
        if (entity == null) return null;
        return TransactionType.valueOf(entity.getName());
    }

    @Named("toCategoryName")
    default String toCategoryName(TransactionCategoryEntity entity) {
        if (entity == null) return null;
        return entity.getName();
    }
}

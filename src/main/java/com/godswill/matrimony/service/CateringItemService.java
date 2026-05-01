package com.godswill.matrimony.service;

import com.godswill.matrimony.model.CateringItem;
import com.godswill.matrimony.repository.CateringItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CateringItemService {

    private final CateringItemRepository cateringItemRepository;

    public List<CateringItem> getAllActive() {
        return cateringItemRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    public List<CateringItem> getAll() {
        return cateringItemRepository.findAllByOrderByDisplayOrderAsc();
    }

    public Optional<CateringItem> findById(String id) {
        return cateringItemRepository.findById(id);
    }

    public CateringItem save(CateringItem item) {
        if (item.getId() == null) {
            item.onCreate();
        } else {
            item.onUpdate();
        }
        return cateringItemRepository.save(item);
    }

    public void deleteById(String id) {
        cateringItemRepository.deleteById(id);
    }
}
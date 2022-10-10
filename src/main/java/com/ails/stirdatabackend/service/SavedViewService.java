package com.ails.stirdatabackend.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ails.stirdatabackend.model.SavedView;
import com.ails.stirdatabackend.payload.SavedViewCreationDTO;
import com.ails.stirdatabackend.payload.SavedViewReturnAllDTO;
import com.ails.stirdatabackend.repository.SavedViewRepository;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SavedViewService {
    
    private final SavedViewRepository savedViewRepository;

    @Autowired
    public SavedViewService(SavedViewRepository savedViewRepository) {
        this.savedViewRepository = savedViewRepository;
    }

    public List<SavedViewReturnAllDTO> getUserSavedViews(ObjectId userId) {
        List<SavedView> viewList = savedViewRepository.findByCreatorId(userId);
        
        List<SavedViewReturnAllDTO> response = viewList.stream()
                                                    .map(view -> new SavedViewReturnAllDTO(view))
                                                    .collect(Collectors.toList());
        
        return response;
    }

    public Optional<SavedView> getSavedViewById(ObjectId userId, ObjectId savedViewId) {
        Optional<SavedView> view = savedViewRepository.findByCreatorIdAndId(userId, savedViewId);
        return view;
    }

    public boolean deleteSavedView(ObjectId userId, ObjectId savedViewId) {
        Optional<SavedView> view = savedViewRepository.findByCreatorIdAndId(userId, savedViewId);
        if (!view.isPresent()) {
            return false;
        }
        savedViewRepository.delete(view.get());
        return true;
    }

    public SavedView createView(ObjectId userId, SavedViewCreationDTO request) {
        SavedView view = new SavedView();
        view.setName(request.getName());
        view.setCreatorId(userId);
        view.setActivity(request.getActivity());
        view.setPlace(request.getPlace());
        view.setFeature(request.getFeature());
        view.setStartDate(request.getStartDate());
        view.setEndDate(request.getEndDate());
        savedViewRepository.save(view);
        return view;
    }
}

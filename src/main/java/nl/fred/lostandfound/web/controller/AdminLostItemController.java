package nl.fred.lostandfound.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.fred.lostandfound.domain.service.LostItemImportService;
import nl.fred.lostandfound.domain.service.LostItemService;
import nl.fred.lostandfound.web.dto.LostItemClaimsReportResponse;
import nl.fred.lostandfound.web.dto.LostItemResponse;
import nl.fred.lostandfound.web.mapper.LostItemClaimsReportMapper;
import nl.fred.lostandfound.web.mapper.LostItemResponseMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/lost-items")
public class AdminLostItemController {

    private final LostItemImportService importService;
    private final LostItemService lostItemService;
    private final LostItemResponseMapper mapper;
    private final LostItemClaimsReportMapper claimsReportMapper;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "/import", consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
    public List<LostItemResponse> importLostItems(@RequestParam("file") final MultipartFile file) {
        log.info("Admin request to import lost items from file '{}'.", file.getOriginalFilename());
        return mapper.toResponses(importService.importFrom(file));
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "/claims", produces = APPLICATION_JSON_VALUE)
    public List<LostItemClaimsReportResponse> findAllWithClaimants() {
        log.info("Admin request to list all lost items with their claimants.");
        return claimsReportMapper.toReports(lostItemService.findAll());
    }

}

package com.zoe.java.ai.langchain4j.controller;

import com.zoe.java.ai.langchain4j.entity.TextileDesignProject;
import com.zoe.java.ai.langchain4j.entity.TextileDesignReview;
import com.zoe.java.ai.langchain4j.entity.TextileDesignerProfile;
import com.zoe.java.ai.langchain4j.result.Result;
import com.zoe.java.ai.langchain4j.service.TextileDesignerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 纺织设计师与纺织设计项目管理接口
 */
@RestController
@RequestMapping("/textile/designer")
@RequiredArgsConstructor
public class TextileDesignerController {

    private final TextileDesignerService textileDesignerService;

    /**
     * 根据姓名查询纺织设计师信息
     */
    @GetMapping("/getByName")
    public Result<TextileDesignerProfile> getDesignerByName(@RequestParam String name) {
        TextileDesignerProfile designer = textileDesignerService.getDesignerByName(name);
        if (designer == null) {
            return Result.fail(404, "未找到纺织设计师信息");
        }
        return Result.ok(designer);
    }

    /**
     * 根据纺织设计师编号查询设计师信息
     */
    @GetMapping("/getByNo")
    public Result<TextileDesignerProfile> getDesignerByNo(@RequestParam String designerNo) {
        TextileDesignerProfile designer = textileDesignerService.getDesignerByNo(designerNo);
        if (designer == null) {
            return Result.fail(404, "未找到纺织设计师信息");
        }
        return Result.ok(designer);
    }

    /**
     * 查询所有纺织设计师列表
     */
    @GetMapping("/list")
    public Result<List<TextileDesignerProfile>> getAllDesigners() {
        List<TextileDesignerProfile> designers = textileDesignerService.getAllDesigners();
        return Result.ok(designers);
    }

    /**
     * 根据专业方向查询纺织设计师
     */
    @GetMapping("/getBySpecialty")
    public Result<List<TextileDesignerProfile>> getDesignersBySpecialty(@RequestParam String specialty) {
        List<TextileDesignerProfile> designers = textileDesignerService.getDesignersBySpecialty(specialty);
        return Result.ok(designers);
    }

    /**
     * 查询纺织设计师的设计项目
     */
    @GetMapping("/designProjects")
    public Result<List<TextileDesignProject>> getDesignProjects(@RequestParam Long designerId) {
        List<TextileDesignProject> projects = textileDesignerService.getDesignProjectsByDesignerId(designerId);
        return Result.ok(projects);
    }

    /**
     * 查询纺织设计师的设计评审列表
     */
    @GetMapping("/reviews")
    public Result<List<TextileDesignReview>> getReviews(@RequestParam Long designerId) {
        List<TextileDesignReview> reviews = textileDesignerService.getReviewsByDesignerId(designerId);
        return Result.ok(reviews);
    }

    /**
     * 查询纺织设计师详细信息（包含档案、设计项目、设计评审）
     */
    @GetMapping("/detail")
    public Result<TextileDesignerService.DesignerDetailVO> getDesignerDetail(@RequestParam Long designerId) {
        TextileDesignerService.DesignerDetailVO detail = textileDesignerService.getDesignerDetail(designerId);
        if (detail == null) {
            return Result.fail(404, "未找到纺织设计师信息");
        }
        return Result.ok(detail);
    }
}

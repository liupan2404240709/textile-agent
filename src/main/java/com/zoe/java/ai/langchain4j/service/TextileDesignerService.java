package com.zoe.java.ai.langchain4j.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zoe.java.ai.langchain4j.entity.TextileDesignProject;
import com.zoe.java.ai.langchain4j.entity.TextileDesignReview;
import com.zoe.java.ai.langchain4j.entity.TextileDesignerProfile;
import com.zoe.java.ai.langchain4j.mapper.TextileDesignProjectMapper;
import com.zoe.java.ai.langchain4j.mapper.TextileDesignReviewMapper;
import com.zoe.java.ai.langchain4j.mapper.TextileDesignerProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 纺织设计师与纺织设计项目管理服务
 */
@Service
@RequiredArgsConstructor
public class TextileDesignerService {

    private final TextileDesignerProfileMapper textileDesignerProfileMapper;
    private final TextileDesignProjectMapper textileDesignProjectMapper;
    private final TextileDesignReviewMapper textileDesignReviewMapper;

    /**
     * 根据纺织设计师姓名查询档案
     */
    public TextileDesignerProfile getDesignerByName(String name) {
        LambdaQueryWrapper<TextileDesignerProfile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TextileDesignerProfile::getName, name)
                   .eq(TextileDesignerProfile::getIsDeleted, 0);
        return textileDesignerProfileMapper.selectOne(queryWrapper);
    }

    /**
     * 根据纺织设计师编号查询档案
     */
    public TextileDesignerProfile getDesignerByNo(String designerNo) {
        LambdaQueryWrapper<TextileDesignerProfile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TextileDesignerProfile::getDesignerNo, designerNo)
                   .eq(TextileDesignerProfile::getIsDeleted, 0);
        return textileDesignerProfileMapper.selectOne(queryWrapper);
    }

    /**
     * 根据纺织设计师ID查询档案
     */
    public TextileDesignerProfile getDesignerById(Long id) {
        return textileDesignerProfileMapper.selectById(id);
    }

    /**
     * 查询所有纺织设计师列表
     */
    public List<TextileDesignerProfile> getAllDesigners() {
        LambdaQueryWrapper<TextileDesignerProfile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TextileDesignerProfile::getIsDeleted, 0)
                   .orderByDesc(TextileDesignerProfile::getRating);
        return textileDesignerProfileMapper.selectList(queryWrapper);
    }

    /**
     * 根据专业方向查询纺织设计师
     */
    public List<TextileDesignerProfile> getDesignersBySpecialty(String specialty) {
        LambdaQueryWrapper<TextileDesignerProfile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(TextileDesignerProfile::getSpecialty, specialty)
                   .eq(TextileDesignerProfile::getIsDeleted, 0);
        return textileDesignerProfileMapper.selectList(queryWrapper);
    }

    /**
     * 查询纺织设计师的设计项目
     */
    public List<TextileDesignProject> getDesignProjectsByDesignerId(Long designerId) {
        LambdaQueryWrapper<TextileDesignProject> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TextileDesignProject::getDesignerId, designerId)
                   .eq(TextileDesignProject::getIsDeleted, 0)
                   .orderByDesc(TextileDesignProject::getCreateTime);
        return textileDesignProjectMapper.selectList(queryWrapper);
    }

    /**
     * 查询纺织设计师的设计评审列表
     */
    public List<TextileDesignReview> getReviewsByDesignerId(Long designerId) {
        LambdaQueryWrapper<TextileDesignReview> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TextileDesignReview::getDesignerId, designerId)
                   .eq(TextileDesignReview::getIsDeleted, 0)
                   .eq(TextileDesignReview::getReviewStatus, 1)
                   .orderByDesc(TextileDesignReview::getCreateTime);
        return textileDesignReviewMapper.selectList(queryWrapper);
    }

    /**
     * 查询纺织设计师的详细信息（包含档案、设计项目、设计评审）
     */
    public DesignerDetailVO getDesignerDetail(Long designerId) {
        TextileDesignerProfile profile = textileDesignerProfileMapper.selectById(designerId);
        if (profile == null) {
            return null;
        }

        DesignerDetailVO detailVO = new DesignerDetailVO();
        detailVO.setProfile(profile);
        detailVO.setDesignProjects(getDesignProjectsByDesignerId(designerId));
        detailVO.setReviews(getReviewsByDesignerId(designerId));

        return detailVO;
    }

    /**
     * 纺织设计师详细信息VO
     */
    @lombok.Data
    public static class DesignerDetailVO {
        private TextileDesignerProfile profile;
        private List<TextileDesignProject> designProjects;
        private List<TextileDesignReview> reviews;
    }
}

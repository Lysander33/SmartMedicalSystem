package com.example.medical.mapper;

import com.example.medical.entity.QaQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 操作在线问答表的接口
 */
@Mapper
public interface QaQuestionMapper {
    int insert(QaQuestion question);
    List<QaQuestion> selectByPatientId(Integer patientId);
    List<QaQuestion> selectAll();
    int answer(@Param("id") Integer id, @Param("answer") String answer);
}

package com.example.medical.mapper;

import com.example.medical.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 操作问诊消息表的接口
 */
@Mapper
public interface MessageMapper {
    int insert(Message message);
    List<Message> selectByConsultationId(Integer consultationId);
}

package com.normal.picturebackend.infrastructure.common;

import com.normal.picturebackend.infrastructure.exception.BusinessException;
import com.normal.picturebackend.infrastructure.exception.ErrorCode;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author normal
 * @date 2025年04月15日 下午12:48
 */
@Data
public class DeleteRequest implements Serializable {


    
    private Long id;

    @Serial
    private static final long serialVersionUID = 1L;


    /**
     * 有效id
     * @return
     */
    public boolean valid() {
        return id > 0;
    }

    /**
     * 无效id
     * @return
     */
    public boolean inValid() {
        return !valid();
    }
}

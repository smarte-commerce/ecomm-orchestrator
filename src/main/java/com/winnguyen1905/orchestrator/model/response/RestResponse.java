package com.winnguyen1905.orchestrator.model.response;

import java.io.Serial;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Builder;

@Builder
@JsonInclude(value = Include.NON_NULL)
public record RestResponse<T>(
    Integer statusCode,
    String error,
    Object message,
    T data)
    implements Serializable {
  
  @Serial
  private static final long serialVersionUID = 7213600440729202783L;
  
  @Builder
  public RestResponse(Integer statusCode, String error, Object message, T data) {
    this.statusCode = statusCode;
    this.error = error;
    this.message = message;
    this.data = data;
  }
} 
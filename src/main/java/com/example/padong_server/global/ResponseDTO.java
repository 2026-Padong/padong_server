package com.example.padong_server.global;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@AllArgsConstructor
@Getter
public class ResponseDTO<T> {
  private final String statusCode;
  private final String message;
  private final T data;

  public static <T> ResponseDTO<T> res(final HttpStatusCode statusCode, final String message) {
    return new ResponseDTO<>(String.valueOf(statusCode.value()), message, null);
  }

  public static <T> ResponseDTO<T> res(final HttpStatusCode statusCode, final String message, final T data) {
    return new ResponseDTO<>(String.valueOf(statusCode.value()), message, data);
  }
}

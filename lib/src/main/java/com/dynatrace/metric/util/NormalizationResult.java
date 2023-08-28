/**
 * Copyright 2023 Dynatrace LLC
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dynatrace.metric.util;

import java.util.function.Supplier;

/**
 * A type that represents the outcome of normalizations (metric key or dimension key/value). It
 * contains the normalized metric key/dimension key/value and error/warning that might have occurred
 * during the process.
 */
final class NormalizationResult {
  private static final NormalizationResult EMPTY_INVALID =
      new NormalizationResult(null, MessageType.ERROR, null);

  private final String result;
  private final MessageType messageType;
  private final Supplier<String> message;

  private NormalizationResult(String result, MessageType messageType, Supplier<String> message) {
    this.result = result;
    this.messageType = messageType;
    this.message = message;
  }

  static NormalizationResult newValid(String result) {
    return new NormalizationResult(result, MessageType.NONE, null);
  }

  static NormalizationResult newValid(String result, Supplier<String> message) {
    return new NormalizationResult(result, MessageType.WARNING, message);
  }

  static NormalizationResult newInvalid() {
    return EMPTY_INVALID;
  }

  static NormalizationResult newInvalid(Supplier<String> message) {
    return new NormalizationResult(null, MessageType.ERROR, message);
  }

  String getResult() {
    return result;
  }

  String getMessage() {
    return message.get();
  }

  MessageType messageType() {
    return messageType;
  }

  enum MessageType {
    WARNING,
    ERROR,
    NONE
  }
}

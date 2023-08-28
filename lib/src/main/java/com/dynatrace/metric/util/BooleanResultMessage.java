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
 * A type that represents if an operation failed or succeeded. It contains the result if it failed
 * or succeeded and error/warning that might have occurred during the process.
 */
final class BooleanResultMessage {
  private static final BooleanResultMessage VALID = new BooleanResultMessage(true, null);

  private final Supplier<String> message;
  private final boolean isValid;

  private BooleanResultMessage(boolean isValid, Supplier<String> message) {
    this.message = message;
    this.isValid = isValid;
  }

  static BooleanResultMessage newValid() {
    return VALID;
  }

  static BooleanResultMessage newInvalid(Supplier<String> message) {
    return new BooleanResultMessage(false, message);
  }

  boolean isValid() {
    return isValid;
  }

  String getMessage() {
    return message.get();
  }
}

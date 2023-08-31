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
package com.dynatrace.testutils;

public class Tuple {

  private final String name;
  private final String input;
  private final String expectedOutput;

  private Tuple(String name, String input, String expectedOutput) {
    this.name = name;
    this.input = input;
    this.expectedOutput = expectedOutput;
  }

  public static Tuple of(String name, String input, String expectedOutput) {
    return new Tuple(name, input, expectedOutput);
  }

  public String getName() {
    return name;
  }

  public String getInput() {
    return input;
  }

  public String getExpectedOutput() {
    return expectedOutput;
  }
}

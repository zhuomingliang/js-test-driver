/*
 * Copyright 2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

goog.provide('jstestdriver.TestResult');

goog.require('jstestdriver');


/**
 * @param {string} testCaseName
 * @param {string} testName
 * @param {jstestdriver.TestResult.RESULT} result
 * @param {string} message
 * @param {Array.<string>} log
 * @param {number} time
 * @param {Object.<string, Object>} opt_data A map of arbitrary value pairs representing test meta data.
 * @constructor
 */
jstestdriver.TestResult = function(testCaseName, testName, result, message, log, time, opt_data) {
  this.testCaseName = testCaseName;
  this.testName = testName;
  this.result = result;
  this.message = message;
  this.log = log;
  this.time = time;
  this.data = opt_data || {};
};


/**
 * @enum {string}
 */
jstestdriver.TestResult.RESULT = {
  PASSED : 'passed',
  ERROR : 'error',
  FAILED : 'failed'
};

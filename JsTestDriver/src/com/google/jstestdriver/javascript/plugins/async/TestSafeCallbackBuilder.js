/*
 * Copyright 2010 Google Inc.
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


/**
 * @fileoverview Defines the TestSafeCallbackBuilder class. It decorates a
 * Javascript function with several safeguards so that it may be safely executed
 * asynchronously within a test.
 *
 * The safeguards include:
 *   1) notifying the test runner about any exceptions thrown when the function
 *      executes
 *   2) restricting the number of times the asynchronous system may call the
 *      function
 *   3) restricting the length of time the asynchronous system may delay before
 *      calling the function
 *
 * @author rdionne@google.com (Robert Dionne)
 */

goog.provide('jstestdriver.plugins.async.TestSafeCallbackBuilder');

goog.require('jstestdriver');
goog.require('jstestdriver.plugins.async.CatchingCallback');
goog.require('jstestdriver.plugins.async.ExpiringCallback');
goog.require('jstestdriver.plugins.async.FiniteUseCallback');
goog.require('jstestdriver.plugins.async.Timeout');

/**
 * Constructs a TestSafeCallbackBuilder.
 *
 * @param {Function} opt_setTimeout the global setTimeout function to use.
 * @param {Function} opt_clearTimeout the global clearTimeout function to use.
 * @param {Function} opt_timeoutConstructor a constructor for obtaining new the
 *     Timeouts.
 * @constructor
 */
jstestdriver.plugins.async.TestSafeCallbackBuilder = function(
    opt_setTimeout, opt_clearTimeout, opt_timeoutConstructor) {
  this.setTimeout_ = opt_setTimeout || jstestdriver.setTimeout;
  this.clearTimeout_ = opt_clearTimeout || jstestdriver.clearTimeout;
  this.timeoutConstructor_ = opt_timeoutConstructor ||
      jstestdriver.plugins.async.Timeout;
  this.callbackDescription = 'Unknown callback.';
  this.stepDescription_ = 'Unknown step.';
  this.pool_ = null;
  this.remainingUses_ = null;
  this.testCase_ = null;
  this.wrapped_ = null;
};


/**
 * Returns the original function decorated with safeguards.
 * @return {*} The return value of the original callback.
 */
jstestdriver.plugins.async.TestSafeCallbackBuilder.prototype.build =
    function() {
  var catchingCallback = new jstestdriver.plugins.async.CatchingCallback(
      this.testCase_, this.pool_, this.wrapped_);
  var timeout = new (this.timeoutConstructor_)(
      this.setTimeout_, this.clearTimeout_);
  var onDepleted = function() {
    timeout.maybeDisarm();
  };
  var finiteUseCallback = new jstestdriver.plugins.async.FiniteUseCallback(
      catchingCallback, onDepleted, this.remainingUses_);
  return new jstestdriver.plugins.async.ExpiringCallback(
      this.pool_, finiteUseCallback, timeout,
      this.stepDescription_, this.callbackDescription_);
};


jstestdriver.plugins.async.TestSafeCallbackBuilder.
    prototype.setCallbackDescription = function(callbackDescription) {
  this.callbackDescription_ = callbackDescription;
  return this;
};


jstestdriver.plugins.async.TestSafeCallbackBuilder.
    prototype.setStepDescription = function(stepDescription) {
  this.stepDescription_ = stepDescription;
  return this;
};


/**
 * @param {jstestdriver.plugins.async.CallbackPool} pool the CallbackPool to
 *     contain the callback.
 * @return {jstestdriver.plugins.async.TestSafeCallbackBuilder} This.
 */
jstestdriver.plugins.async.TestSafeCallbackBuilder.prototype.setPool = function(
    pool) {
  this.pool_ = pool;
  return this;
};


/**
 * @param {number} remainingUses The remaining number of permitted calls.
 * @return {jstestdriver.plugins.async.TestSafeCallbackBuilder} This.
 */
jstestdriver.plugins.async.TestSafeCallbackBuilder.prototype.setRemainingUses =
    function(remainingUses) {
  this.remainingUses_ = remainingUses;
  return this;
};


/**
 * @param {Object} testCase The test case instance available as 'this' within
 *     the function's scope.
 * @return {jstestdriver.plugins.async.TestSafeCallbackBuilder} This.
 */
jstestdriver.plugins.async.TestSafeCallbackBuilder.prototype.setTestCase =
    function(testCase) {
  this.testCase_ = testCase;
  return this;
};


/**
 * @param {Function} wrapped The function wrapped by the above safeguards.
 * @return {jstestdriver.plugins.async.TestSafeCallbackBuilder} This.
 */
jstestdriver.plugins.async.TestSafeCallbackBuilder.prototype.setWrapped =
    function(wrapped) {
  this.wrapped_ = wrapped;
  return this;
};

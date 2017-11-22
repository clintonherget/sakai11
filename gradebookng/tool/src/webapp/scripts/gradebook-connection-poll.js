/**************************************************************************************
 *                    Connection Poll Javascript                                       
 *************************************************************************************/
function ConnectionPoll($message) {
    this.PING_INTERVAL = 1000*5; // 5 seconds
    this.PING_TIMEOUT = 1000*10; // 10 seconds
    this.PING_URL = "/direct/gbng/ping";

    this._timeout = false;
    this._timeoutCallbacks = [];
    this._recoverCallbacks = [];

    this.$message = $message;

    this.poll();
};


ConnectionPoll.prototype.poll = function() {
  var self = this;

  self._interval = setInterval(function() {
    self.ping();
  }, self.PING_INTERVAL);
};


ConnectionPoll.prototype.ping = function() {
  $.ajax({
    type: "GET",
    url: this.PING_URL,
    timeout: this.PING_TIMEOUT,
    cache: false,
    success: $.proxy(this.handleSuccess, this),
    error: $.proxy(this.handleTimeout, this)
  });
};

ConnectionPoll.prototype.handleTimeout = function() {
  if (this._timeout) {
    return;
  }

  this._timeout = true;

  this.$message.show();

  this._timeoutCallbacks.forEach(function(callback) {
    callback();
  });
};

ConnectionPoll.prototype.handleSuccess = function() {
  this.$message.hide();

  if (this._timeout) {
    this._recoverCallbacks.forEach(function(callback) {
      callback();
    });
  }

  this._timeout = false;
};

ConnectionPoll.prototype.onTimeout = function(callback) {
  this._timeoutCallbacks.push(callback);
  return this;
};

ConnectionPoll.prototype.onRecover = function(callback) {
  this._recoverCallbacks.push(callback);
  return this;
};

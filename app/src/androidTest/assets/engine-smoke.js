export default {
  init() {},
  home() { return JSON.stringify({class:[{type_id:'1',type_name:'JS smoke'}],list:[{vod_id:'js',vod_name:'JS smoke'}]}); },
  play(flag, id) { return JSON.stringify({parse:0,url:'js-fixture'}); }
};

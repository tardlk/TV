from base.spider import Spider as BaseSpider

class Spider(BaseSpider):
    def init(self, extend=""):
        self.extend = extend
    def homeContent(self, filter):
        return {"class": [{"type_id": "1", "type_name": "Python smoke"}], "list": [{"vod_id": "python", "vod_name": "Python smoke"}]}
    def playerContent(self, flag, id, vipFlags):
        return {"parse": 0, "url": "python-fixture"}
    def destroy(self):
        pass

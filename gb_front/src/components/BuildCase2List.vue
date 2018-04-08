<template>
    <div>
        <div  class="row">
          <div class="form-horizontal">
                <div class="form-group">
                    <label class="col-sm-2 control-label">關鍵字:</label>
                    <div class="col-sm-10">
                      <input type="text" v-model="keyword">
                    </div>
                </div>
            </div>
        </div>
        <div v-if="buildCaseList.length != 0" class="table-responsive">
            <table class="table table-hover table-bordered table-condensed">
                <thead>
                    <tr class="info">
                        <th></th>
                        <th @click="toggleSort('permitDate')"><a>發照日期&nbsp;<span v-html = "sortDir('permitDate')"></span></a></th>
                        <th @click="toggleSort('_id.county')"><a>縣市&nbsp;<span v-html = "sortDir('_id.county')"></span></a></th>
                        <th @click="toggleSort('builder')"><a>起造人&nbsp;<span v-html = "sortDir('builder')"></span></a></th>
                        <th @click="toggleSort('architect')"><a>建築師&nbsp;<span v-html = "sortDir('architect')"></span></a></th>
                        <th @click="toggleSort('siteInfo.area')"><a>樓板面積&nbsp;<span v-html = "sortDir('siteInfo.area')"></span></a></th>
                        <th @click="toggleSort('siteInfo.addr')"><a>地號&nbsp;<span v-html = "sortDir('siteInfo.addr')"></span></a></th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="(buildCase, index) in buildCaseList" :key="buildCase._id.permitID" :class='{success: selectedIndex == index}'>
                        <td>
                            <button class="btn btn-primary" @click="editBuildCase(index)">
                                <i class="fa fa-pen"></i>&nbsp;細節</button>
                            <button class="btn btn-primary" @click="obtainCase(buildCase)" v-if="obtainBtn">
                                <i class="fa fa-pen"></i>&nbsp;取得</button>  
                            <button class="btn btn-primary" @click="releaseCase(buildCase)" v-if="!obtainBtn">
                                <i class="fa fa-pen"></i>&nbsp;歸還</button> 
                            <button class="btn btn-primary" @click="caseMap(index)" :disabled="!buildCase.location">
                                <i class="fa fa-pen"></i>&nbsp;地圖</button>                                                                  
                        </td>
                        <td>{{ issueDate(buildCase)}}</td>
                        <td>{{ buildCase._id.county}}</td>
                        <td>{{ buildCase.builder}}</td>
                        <td>{{ buildCase.architect}}</td>
                        <td>{{ buildCase.siteInfo.area}}</td>
                        <td>{{ buildCase.siteInfo.addr}}</td>
                    </tr>
                </tbody>
            </table>
            <pagination for="cardList" :records="total" :per-page="5" count-text="第{from}到第{to}筆/共{count}筆|{count} 筆|1筆" @paginate="setPage"></pagination>
            <div>
              <button  v-if="download" class="btn btn-primary" @click="downloadExcel()">下載Excel</button>
              <button v-if="dm" class="btn btn-primary" @click="downloadDM()">下載DM 信封</button>
              <button v-if="splitCase" class="btn btn-primary" @click="splitCaseList()">平均分配名單</button>
            </div>
        </div>
        <div v-else class="alert alert-info" role="alert">無</div>
        <build-case2-detail v-if="display === 'detail'" :buildCase="buildCaseList[selectedIndex]" @Changed="reload"></build-case2-detail>
        <build-case2-map v-if="display === 'map'" :buildCase="buildCaseList[selectedIndex]"></build-case2-map>
    </div>
</template>
<style scoped>

</style>
<script>
import axios from "axios";
import moment from "moment";
import { Pagination, PaginationEvent } from "vue-pagination-2";
import BuildCase2Detail from "./BuildCase2Detail.vue";
import BuildCase2Map from "./BuildCase2Map.vue";
import baseUrl from "../baseUrl";

export default {
  props: {
    url: {
      type: String,
      required: true
    },
    param: {
      type: Object,
      required: true
    },
    obtainBtn: {
      type: Boolean,
      default: false
    },
    download: {
      type: Boolean,
      default: false
    },
    dm: {
      type: Boolean,
      default: false
    },
    splitCase: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      buildCaseList: [],
      limit: 5,
      total: 0,
      display: "",
      selectedIndex: -1,
      sortBy: "siteInfo.area+",
      keyword: "",
      page: 0
    };
  },
  computed: {},
  mounted: function() {
    this.fetchBuildCase(0, this.limit);
    PaginationEvent.$on("vue-pagination::cardList", this.handlePageChange);
  },
  watch: {
    url: function(newUrl) {
      this.fetchBuildCase(0, this.limit);
    },
    param: function(newParam) {
      this.fetchBuildCase(0, this.limit);
    },
    keyword(newKeyword) {
      this.fetchBuildCase(0, this.limit);
    }
  },

  methods: {
    setPage(page) {
      this.page = page;
    },
    reload() {
      this.handlePageChange(this.page);
    },
    processResp(resp) {
      const ret = resp.data;
      this.buildCaseList.splice(0, this.buildCaseList.length);

      for (let buildCase of ret) {
        this.buildCaseList.push(buildCase);
      }
    },
    fetchBuildCase(skip, limit) {
      let paramJson = JSON.stringify(
        Object.assign(this.param, {
          keyword: this.keyword,
          sortBy: this.sortBy
        })
      );
      let request_url = `${this.url}/${encodeURIComponent(
        paramJson
      )}/${skip}/${limit}`;

      axios
        .get(request_url)
        .then(this.processResp)
        .catch(err => {
          alert(err);
        });

      this.fetchBuildCaseCount();
    },
    fetchBuildCaseCount() {
      let paramJson = JSON.stringify(
        Object.assign(this.param, {
          keyword: this.keyword,
          sortBy: this.sortBy
        })
      );
      let request_url = `${this.url}/${encodeURIComponent(paramJson)}/count`;
      axios
        .get(request_url)
        .then(resp => {
          this.total = resp.data;
        })
        .catch(err => {
          alert(err);
        });
    },
    handlePageChange(page) {
      let skip = (page - 1) * this.limit;
      this.fetchBuildCase(skip, this.limit);
    },
    editBuildCase(idx) {
      this.selectedIndex = idx;
      this.display = "detail";
    },
    caseMap(idx) {
      this.selectedIndex = idx;
      this.display = "map";
    },

    issueDate(buildCase) {
      let dateStr = moment(buildCase.permitDate).format("LL");
      return moment(buildCase.permitDate).fromNow() + `(${dateStr})`;
    },
    obtainCase(buildCase) {
      let _id = buildCase._id;
      axios
        .post("/ObtainCase", _id)
        .then(resp => {
          const ret = resp.data;
          if (ret.ok) {
            alert("成功取得");
            this.fetchBuildCase(0, this.limit);
          } else {
            alert(ret.msg);
          }
        })
        .catch(err => alert(err));
    },
    releaseCase(buildCase) {
      let _id = buildCase._id;
      axios
        .post("/ReleaseCase", _id)
        .then(resp => {
          const ret = resp.data;
          if (ret.ok) {
            alert("成功歸還");
            this.fetchBuildCase(0, this.limit);
          } else {
            alert(ret.msg);
          }
        })
        .catch(err => alert(err));
    },
    toggleSort(col) {
      if (this.sortBy.indexOf(col) == -1) {
        this.sortBy = `${col}+`;
      } else {
        if (this.sortBy.indexOf("+") != -1)
          this.sortBy = this.sortBy.replace("+", "-");
        else {
          this.sortBy = this.sortBy.replace("-", "+");
        }
      }

      this.fetchBuildCase(0, this.limit);
    },
    sortDir(col) {
      if (this.sortBy.indexOf(col) == -1) {
        return "";
      } else if (this.sortBy.indexOf("-") != -1)
        return '<i class="fa fa-sort-desc" aria-hidden="true"></i>';
      else return '<i class="fa fa-sort-asc" aria-hidden="true"></i>';
    },
    downloadExcel() {
      let paramJson = JSON.stringify(
        Object.assign(this.param, { sortBy: this.sortBy })
      );
      let url =
        baseUrl() + `${this.url}/${encodeURIComponent(paramJson)}/excel`;
      window.open(url);
    },
    downloadDM() {
      let url = baseUrl() + `${this.url}/dm`;
      window.open(url);
    },
    splitCaseList() {
      let url = baseUrl() + `${this.url}/split`;
      axios.get(url).then(resp => {
        let ret = resp.data;
        let msg = `${ret.updated}個名單被分派`;
        alert(msg);
      });
    }
  },
  components: {
    Pagination,
    BuildCase2Detail,
    BuildCase2Map
  }
};
</script>

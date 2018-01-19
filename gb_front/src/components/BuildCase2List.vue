<template>
    <div>
        <div v-if="buildCaseList.length != 0" class="table-responsive">
            <table class="table table-hover table-bordered table-condensed">
                <thead>
                    <tr class="info">
                        <th></th>
                        <th @click="toggleSort('permitDate')"><a>發照日期</a></th>
                        <th @click="toggleSort('county')"><a>縣市</a></th>
                        <th @click="toggleSort('builder')"><a>起造人</a></th>
                        <th @click="toggleSort('architect')"><a>建築師</a></th>
                        <th @click="toggleSort('area')"><a>樓板面積</a></th>
                        <th @click="toggleSort('addr')"><a>地號</a></th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="(buildCase, index) in buildCaseList" :class='{success: selectedIndex == index}'>
                        <td>
                            <button class="btn btn-primary" @click="editBuildCase(index)">
                                <i class="fa fa-pen"></i>&nbsp;細節</button>
                            <button class="btn btn-primary" @click="obtainCase(buildCase)" v-if="obtainBtn">
                                <i class="fa fa-pen"></i>&nbsp;取得</button>  
                            <button class="btn btn-primary" @click="releaseCase(buildCase)" v-if="!obtainBtn">
                                <i class="fa fa-pen"></i>&nbsp;歸還</button> 
                            <button class="btn btn-primary" @click="caseMap(index)">
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
            <pagination for="cardList" :records="total" :per-page="5" count-text="第{from}到第{to}筆/共{count}筆|{count} 筆|1筆"></pagination>
        </div>
        <div v-else class="alert alert-info" role="alert">無</div>
        <build-case2-detail v-if="display === 'detail'" :buildCase="buildCaseList[selectedIndex]"></build-case2-detail>
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
    }
  },
  data() {
    return {
      buildCaseList: [],
      limit: 5,
      total: 0,
      display: "",
      selectedIndex: -1
    };
  },
  computed: {
    sortDir() {
      if (this.param.sortBy.indexOf("+") != -1) return "[+]";
      else return "[-]";
    }
  },
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
    }
  },

  methods: {
    processResp(resp) {
      const ret = resp.data;
      this.buildCaseList.splice(0, this.buildCaseList.length);

      for (let buildCase of ret) {
        this.buildCaseList.push(buildCase);
      }
    },
    fetchBuildCase(skip, limit) {
      let request_url = `${this.url}/${skip}/${limit}`;

      if (this.param) {
        axios
          .post(request_url, this.param)
          .then(this.processResp)
          .catch(err => {
            alert(err);
          });
      } else {
        axios
          .get(request_url)
          .then(this.processResp)
          .catch(err => {
            alert(err);
          });
      }
      this.fetchBuildCaseCount();
    },
    fetchBuildCaseCount() {
      let request_url = `${this.url}/count`;
      if (this.param) {
        axios
          .post(request_url, this.param)
          .then(resp => {
            this.total = resp.data;
          })
          .catch(err => {
            alert(err);
          });
      } else {
        axios
          .get(request_url)
          .then(resp => {
            this.total = resp.data;
          })
          .catch(err => {
            alert(err);
          });
      }
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
    builderType(buildCase) {
      if (buildCase.personal) return "個人";
      else return "公司";
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
      if (this.param.sortBy.indexOf(col) == -1) {
        this.param.sortBy = `${col}+`;
      } else {
        if (this.param.sortBy.indexOf("+") == -1)
          this.param.sortBy = this.param.sortBy.replace("-", "+");
        else this.param.sortBy = this.param.sortBy.replace("+", "-");
      }
      this.fetchBuildCase(0, this.limit);
    },
    headerPrompt(col) {}
  },
  components: {
    Pagination,
    BuildCase2Detail,
    BuildCase2Map
  }
};
</script>

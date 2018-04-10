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
        <div v-if="facilityList.length != 0" class="table-responsive">
            <table class="table table-hover table-bordered table-condensed">
                <thead>
                    <tr class="info">
                        <th></th>
                        <th @click="toggleSort('name')"><a>名稱&nbsp;<span v-html = "sortDir('name')"></span></a></th>
                        <th @click="toggleSort('county')"><a>縣市&nbsp;<span v-html = "sortDir('county')"></span></a></th>
                        <th @click="toggleSort('phone')"><a>環安人員&nbsp;<span v-html = "sortDir('contact')"></span></a></th>
                        <th @click="toggleSort('phone')"><a>電話&nbsp;<span v-html = "sortDir('phone')"></span></a></th>
                        <th @click="toggleSort('pollutant.noVOCtotal')"><a>不含VOC空汙(噸)&nbsp;<span v-html = "sortDir('pollutant.noVOCtotal')"></span></a></th>
                        <th @click="toggleSort('addr')"><a>地址&nbsp;<span v-html = "sortDir('addr')"></span></a></th>                        
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="(facility, index) in facilityList" :class='{success: selectedIndex == index}' :key="facility._id">
                        <td>
                            <button class="btn btn-primary" @click="editFacility(index)">
                                <i class="fa fa-pen"></i>&nbsp;細節</button>
                            <button class="btn btn-primary" @click="obtainCase(facility)" v-if="obtainBtn">
                                <i class="fa fa-pen"></i>&nbsp;取得</button>  
                            <button class="btn btn-primary" @click="releaseCase(facility)" v-if="!obtainBtn">
                                <i class="fa fa-pen"></i>&nbsp;歸還</button> 
                            <button class="btn btn-primary" @click="caseMap(index)" :disabled="!facility.location">
                                <i class="fa fa-pen"></i>&nbsp;地圖</button>                                                                  
                            <button class="btn btn-info" @click="facilityExcel(facility)">
                                <i class="fa fa-excel"></i>&nbsp;Excel</button>                                                                      
                        </td>
                        <td>{{ facility.name}}</td>
                        <td>{{ facility.county}}</td>
                        <td>{{ facility.contact}}</td>
                        <td>{{ facility.phone}}</td>
                        <td>{{ displayPollutant(facility.pollutant)}}</td>
                        <td>{{ facility.addr}}</td>
                    </tr>
                </tbody>
            </table>
            <pagination for="cardList" :records="total" :per-page="5" count-text="第{from}到第{to}筆/共{count}筆|{count} 筆|1筆"></pagination>
            <div>
              <button v-if="download" class="btn btn-primary" @click="downloadExcel()">下載Excel</button>
              <button v-if="splitCase" class="btn btn-primary" @click="splitCaseList()">平均分配名單</button>
            </div>            
        </div>
        <div v-else class="alert alert-info" role="alert">無</div>
        <facility-detail v-if="display === 'detail'" :facility="facilityList[selectedIndex]"></facility-detail>
        <build-case2-map v-if="display === 'map'" :facility="facilityList[selectedIndex]"></build-case2-map>
    </div>
</template>
<style scoped>

</style>
<script>
import axios from "axios";
import moment from "moment";
import FacilityDetail from "./FacilityDetail.vue";
import { Pagination, PaginationEvent } from "vue-pagination-2";
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
    splitCase: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      facilityList: [],
      limit: 5,
      total: 0,
      display: "",
      selectedIndex: -1,
      sortBy: "bed-",
      keyword: ""
    };
  },
  computed: {},
  mounted: function() {
    this.fetchCareHouse(0, this.limit);
    PaginationEvent.$on("vue-pagination::cardList", this.handlePageChange);
  },
  watch: {
    url: function(newUrl) {
      this.fetchCareHouse(0, this.limit);
    },
    param: function(newParam) {
      this.fetchCareHouse(0, this.limit);
    },
    keyword(newKeyword) {
      this.fetchCareHouse(0, this.limit);
    }
  },

  methods: {
    processResp(resp) {
      const ret = resp.data;
      this.facilityList.splice(0, this.facilityList.length);

      for (let facility of ret) {
        this.facilityList.push(facility);
      }
    },
    fetchCareHouse(skip, limit) {
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

      this.fetchCareHouseCount();
    },
    fetchCareHouseCount() {
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
      this.fetchCareHouse(skip, this.limit);
    },
    editFacility(idx) {
      this.selectedIndex = idx;
      this.display = "detail";
    },
    caseMap(idx) {
      this.selectedIndex = idx;
      this.display = "map";
    },
    obtainCase(facility) {
      let _id = facility._id;
      axios
        .post("/ObtainCase", _id)
        .then(resp => {
          const ret = resp.data;
          if (ret.ok) {
            alert("成功取得");
            this.fetchCareHouse(0, this.limit);
          } else {
            alert(ret.msg);
          }
        })
        .catch(err => alert(err));
    },
    releaseCase(facility) {
      let _id = facility._id;
      axios
        .post("/ReleaseCase", _id)
        .then(resp => {
          const ret = resp.data;
          if (ret.ok) {
            alert("成功歸還");
            this.fetchCareHouse(0, this.limit);
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

      this.fetchCareHouse(0, this.limit);
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
    splitCaseList() {
      let url = baseUrl() + `${this.url}/split`;
      axios.get(url).then(resp => {
        let ret = resp.data;
        let msg = `${ret.updated}個名單被分派`;
        alert(msg);
      });
    },
    displayPollutant(air) {
      if (air) {
        if (air.noVOCtotal) return air.noVOCtotal;
        else return 0;
      } else return 0;
    },
    facilityExcel(facility) {
      let url = baseUrl() + `/FacilityExcel/${facility._id}`;
      window.open(url);
    }
  },
  components: {
    FacilityDetail,
    Pagination
  }
};
</script>

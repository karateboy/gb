<template>
    <div>
        <div class="alert alert-success">
          <strong>{{thisMonth()}}</strong>
        </div>
        <div class="form-horizontal">
            <div class="form-group">
                <label class="col-sm-2 control-label">起造人數量:</label>
                <div class="col-sm-4">
                    <input type="text" class="form-control"
                           readonly :value="report.builder.length">
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">建案數量:</label>
                <div class="col-sm-4">
                    <input type="text" class="form-control"
                           readonly :value="report.buildCase.length">
                </div>
            </div>
            <div class="form-group">
            <div class="col-sm-offset-2 col-sm-4">
              <button class="btn btn-primary" @click.prevent="showDetail">顯示細節</button>
              <button class="btn btn-primary" @click.prevent="subOffset">上月</button>
              <button class="btn btn-primary" @click.prevent="addOffset">次月</button>
            </div>
          </div>
        </div>
        <div v-if="detail">
          <table>
            <thead>
              <tr><th>序號</th><th>內容</th></tr>
            </thead>
            <tbody>
              <tr><th>起造人</th><th></th></tr>
              <tr v-for="(bd, index) in report.builder">
                <td>{{index +1}}</td>
                <td>{{bd}}</td>
              </tr>
              <tr><th>建案</th><th></th></tr>
              <tr v-for="(bc, index) in report.buildCase">
                <td>{{index +1}}</td>
                <td>{{bc.county + ":" + bc.permitID}}</td>
              </tr>
            </tbody>
          </table>
        </div>        
    </div>
</template>
<style>
body {
  background-color: #ff0000;
}
</style>
<script>
import moment from "moment";
import axios from "axios";
import { mapActions } from "vuex";

export default {
  data() {
    return {
      offset: 0,
      report: {
        buildCase: [],
        builder: []
      },
      detail: false
    };
  },
  computed: {},
  mounted() {
    this.getReport();
  },
  methods: {
    getReport() {
      axios
        .get("/UsageRecord/" + this.offset)
        .then(resp => {
          const ret = resp.data;
          const status = resp.status;
          this.report.buildCase.splice(0, this.report.buildCase.length);
          if (ret.buildCase) {
            for (let bc of ret.buildCase) this.report.buildCase.push(bc);
          }

          this.report.builder.splice(0, this.report.builder.length);
          if (ret.builder) {
            for (let bd of ret.builder) this.report.builder.push(bd);
          }
        })
        .catch(err => alert(err));
    },
    addOffset() {
      this.offset += 1;
      this.getReport();
    },
    subOffset() {
      this.offset -= 1;
      this.getReport();
    },
    thisMonth() {
      return moment()
        .add(this.offset, "months")
        .format("YYYY年MM月");
    },
    showDetail() {
      this.detail = true;
    }
  },
  components: {}
};
</script>

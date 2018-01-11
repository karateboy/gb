<template>
    <div>
        <div class="alert alert-info">
          <strong>找不到地號, 面積和經緯度都填0</strong>
        </div>
        <div class="form-horizontal">
            <div class="form-group">
                <label class="col-sm-2 control-label">建案縣市:</label>
                <div class="col-sm-4">
                    <input type="text" class="form-control"
                           readonly :value="buildCase._id.county">
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">建照:</label>
                <div class="col-sm-4">
                    <input type="text" class="form-control"
                           readonly :value="buildCase._id.permitID">
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">地號:</label>
                <div class="col-sm-4">
                    <input type="text" placeholder="地號" class="form-control"
                           readonly :value="buildCase.siteInfo.addr">
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">面積:</label>
                <div class="col-sm-4">
                    <input type="number" placeholder="面積" class="form-control"
                           required v-model.number="buildCase.siteInfo.area">
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">經度 (120~124):</label>
                <div class="col-sm-4">
                    <input type="number" placeholder="120" class="form-control"
                           required v-model.number="buildCase.location[0]">
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">緯度 (22~25):</label>
                <div class="col-sm-4">
                    <input type="number" placeholder="25" class="form-control"
                           required v-model.number="buildCase.location[1]">
                </div>
            </div>

            <div class="form-group">
                <div class="col-sm-offset-2 col-sm-2">
                    <button class="btn btn-primary" @click.prevent="updateBuildCase">更新</button>
                </div>
            </div>
        </div>
    </div>
</template>
<style>
body {
  background-color: #ff0000;
}
</style>
<script>
import axios from "axios";
import { mapActions } from "vuex";

export default {
  data() {
    return {
      buildCase: {
        _id: {
          county: "",
          permitID: ""
        },
        siteInfo: {
          addr: "",
          area: 0
        },
        location: [0, 0]
      }
    };
  },
  computed: {
  },
  mounted() {
    this.checkOut();
  },
  methods: {
    checkOut() {
      axios
        .get("/CheckOutBuildCase")
        .then(resp => {
          const ret = resp.data;
          const status = resp.status;
          if (status == 200) {
            this.buildCase = JSON.parse(JSON.stringify(ret));

            if (!this.buildCase.location) this.buildCase.location = [0, 0];

            if (!this.buildCase.siteInfo.area) this.buildCase.siteInfo.area = 0;
          } else {
            alert("已無建案待更新, 請稍後再試");
          }
        })
        .catch(err => alert(err));
    },
    updateBuildCase() {
      axios
        .post("/BuildCase", this.buildCase)
        .then(resp => {
          const ret = resp.data;
          if (ret.ok) {
            alert("成功");
            this.checkOut();
          } else alert("失敗:" + ret.msg);
        })
        .catch(err => {
          alert(err);
        });
    }
  },
  components: {}
};
</script>

<template>
    <div>
        <div class="alert alert-info">
          <strong><a href="http://cpabm.cpami.gov.tw/cers/Welcome.do" target="_blank">承造人名單查詢網頁</a></strong>
        </div>
        <div class="form-horizontal">
            <div class="form-group">
                <label class="col-sm-2 control-label">建案縣市:</label>
                <div class="col-sm-6">
                    <input type="text" class="form-control"
                           readonly :value="buildCase._id.county">
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">建照:</label>
                <div class="col-sm-6">
                    <input type="text" class="form-control"
                           readonly :value="buildCase._id.permitID">
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">建築地點:</label>
                <div class="col-sm-6">
                    <input type="text" class="form-control"
                           readonly :value="buildCase.siteInfo.addr">
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">起造人:</label>
                <div class="col-sm-6">
                    <input type="text" class="form-control"
                           readonly :value="buildCase.builder">
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">承造人:</label>
                <div class="col-sm-6">
                    <input type="text" placeholder="承造人" class="form-control"
                           v-model="buildCase.contractor">
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">承造人地址:</label>
                <div class="col-sm-6">
                    <input type="text" placeholder="地址" class="form-control"
                           v-model="contractor.addr">
                </div>
            </div>                        
            <div class="form-group">
                <div class="col-sm-offset-2 col-sm-2">
                    <button class="btn btn-primary" @click.prevent="updateBuildCase">更新</button>
                </div>
                <div class="col-sm-offset-2 col-sm-2">
                    <button class="btn btn-primary" @click.prevent="skipContractor">尚無承造人資訊</button>
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
        contractor: undefined
      },
      contractor: {
        _id: "",
        addr: undefined,
        phone: undefined,
        contact: undefined
      }
    };
  },
  computed: {},
  mounted() {
    this.checkOut();
  },
  watch: {
    "buildCase.contractor": function(newContractor) {
      if (newContractor) this.getContractor(newContractor);
    }
  },
  methods: {
    checkOut() {
      axios
        .get("/CheckOutContractor")
        .then(resp => {
          const ret = resp.data;
          const status = resp.status;
          if (status == 200) {
            this.buildCase = JSON.parse(JSON.stringify(ret));
            this.contractor._id = "";
            this.contractor.addr = undefined;
            this.contractor.phone = undefined;
            this.contractor.contact = undefined;
          } else {
            alert("已承造人待更新, 請稍後再試");
          }
        })
        .catch(err => alert(err));
    },
    getContractor(id) {
      let url = `/Contractor/` + encodeURIComponent(id);
      axios.get(url).then(resp => {
        const ret = resp.data;
        const status = resp.status;
        if (status === 200) {
          this.contractor._id = ret._id;
          this.contractor.addr = ret.addr;
          this.contractor.phone = ret.phone;
        } else {
          this.contractor._id = "";
          this.contractor.addr = undefined;
          this.contractor.phone = undefined;
          this.contractor.contact = undefined;
        }
      });
    },
    skipContractor() {
      this.buildCase.contractor = undefined;
      this.buildCase.contractorCheckDate = new Date();
      this.updateBuildCase();
    },
    upsertContractor() {
      this.contractor._id = this.buildCase.contractor;
      axios
        .post("/Contractor", this.contractor)
        .then(resp => {
          const ret = resp.data;
          if (ret.ok) {
            console.log("承造人成功更新");
          } else console.log("失敗:" + ret.msg);
        })
        .catch(err => {
          alert(err);
        });
    },
    updateBuildCase() {
      if (this.buildCase.contractor) this.upsertContractor();

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

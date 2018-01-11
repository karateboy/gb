<template>
    <div>
        <div class="form-horizontal">
            <div class="form-group">
                <label class="col-sm-2 control-label">起造人名稱:</label>
                <div class="col-sm-4">
                    <input type="text" placeholder="起造人名稱" class="form-control"
                           required v-model="buildCase._id">
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">地址:</label>
                <div class="col-sm-4">
                    <input type="text" placeholder="地址" class="form-control"
                           required v-model="buildCase.addr">
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">聯絡人:</label>
                <div class="col-sm-4">
                    <input type="text" placeholder="地址" class="form-control"
                           required v-model="buildCase.contact">
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">電話:</label>
                <div class="col-sm-4">
                    <input type="text" placeholder="電話" class="form-control"
                           required v-model="buildCase.phone">
                </div>
            </div>

            <div class="form-group">
                <div class="col-sm-offset-2 col-sm-2">
                    <button class="btn btn-primary" @click.prevent="submit" :disabled="!buildCase.phone">更新</button>
                    <button class="btn btn-primary" @click.prevent="giveUp">找不到電話</button>
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
      buildCase: {}
    };
  },
  computed: {},
  mounted(){
      this.checkOut()
  },
  methods: {
    checkOut() {
      axios
        .get("/CheckOutBuildCase")
        .then(resp => {            
          const ret = resp.data;
          const status = resp.status
          if(status == 200){
            this.buildCase = JSON.parse(JSON.stringify(ret));
            console.log(this.buildCase)
          }            
          else{
            alert("已無建案需要更新, 請稍後再試")
          }
        })
        .catch(err => alert(err));
    },
    updateBuildCase() {
      console.log(this.buildCase);

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

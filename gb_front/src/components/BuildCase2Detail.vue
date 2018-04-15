<template>
    <div>
        <br>
        <div class="form-horizontal">
            <div class="form-group">
                <label class="col-sm-1 control-label">縣市:</label>
                <div class="col-sm-4"><input type="text" class="form-control" :value="buildCase._id.county" readonly></div>
            </div>
            <div class="form-group">
                <label class="col-sm-1 control-label">建照:</label>
                <div class="col-sm-4"><input type="text" class="form-control" :value="buildCase._id.permitID" readonly></div>
            </div>
            <div class="form-group">
                <label class="col-sm-1 control-label">地號:</label>
                <div class="col-sm-4"><input type="text" class="form-control" :value="buildCase.siteInfo.addr" readonly></div>
            </div>
            <div class="form-group">
                <label class="col-sm-1 control-label">樓地板面積:</label>
                <div class="col-sm-4"><input type="number" class="form-control" v-model.number="buildCase.siteInfo.area"></div>
            </div>
            <div class="form-group">
                <label class="col-sm-1 control-label">用途:</label>
                <div class="col-sm-4"><input type="text" class="form-control" :value="buildCase.siteInfo.usage" readonly></div>
            </div>
            <div class="form-group">
                <label class="col-sm-1 control-label">起造人:</label>
                <div class="col-sm-4"><input type="text" class="form-control" :value="buildCase.builder" readonly></div>
            </div>
            <div v-if="displayBuilder" class="form-group">
                <label class="col-sm-1 control-label">聯絡資訊:</label>
                <div class="col-sm-4">
                    <builder :builder="builder"></builder>
                </div>
            </div>
            
            <div class="form-group">
                <label class="col-sm-1 control-label">建築師:</label>
                <div class="col-sm-4"><input type="text" class="form-control" :value="buildCase.architect" readonly></div>
            </div>            
            <div class="form-group">
                <label class="col-sm-1 control-label">承造單位:</label>
                <div class="col-sm-4"><input type="text" class="form-control" :value="buildCase.contractor" readonly></div>
            </div>
            <div class="form-group">
                <label class="col-sm-1 control-label">業務:</label>
                <div class="col-sm-4"><input type="text" class="form-control" :value="buildCase.owner" readonly></div>
            </div>
            <div class="form-group">
                <label class="col-sm-1 control-label">座標:</label>
                <div class="col-sm-4"><input type="text" class="form-control" v-model="location"></div>
            </div>
            <div class="form-group">
              <div class="col-sm-1 col-sm-offset-1">
                    <button class='btn btn-primary' @click='save'>更新建案資訊</button>
              </div>
            </div>
            <build-case-form :buildCaseID="buildCase._id" :build-case-form="buildCase.form" @formChanged="onFormChanged"></build-case-form>
        </div>
        <div>
        
        </div>

    </div>
</template>
<style scoped>

</style>
<script>
import axios from "axios";
import moment from "moment";
import { mapGetters } from "vuex";
import Builder from "./Builder.vue";
import BuildCaseForm from "./BuildCaseForm.vue";

export default {
  props: {
    buildCase: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      //displayBuilder: false,
      builder: {},
      ioType: "in",
      opType: "add",
      entryIndex: 0,
      entry: {}
    };
  },
  computed: {
    ...mapGetters(["user"]),
    displayBuilder: function() {
      if (!this.buildCase.personal) {
        axios
          .get("/Builder/" + encodeURI(this.buildCase.builder))
          .then(resp => {
            if (resp.status == 200) {
              const ret = resp.data;
              this.builder = ret;
            }
          });
        return true;
      } else return false;
    },
    location: {
      get: function() {
        if (this.buildCase.location) {
          let ret = this.buildCase.location.slice();
          return ret.reverse().join();
        } else return "";
      },
      set: function(v) {
        if (v) {
          let locationStr = v.split(",");
          this.buildCase.location.splice(0, this.buildCase.location.length);
          this.buildCase.location.push(parseFloat(locationStr[1]));
          this.buildCase.location.push(parseFloat(locationStr[0]));
        }
      }
    }
  },
  methods: {
    noteHeader(note) {
      let date = moment(note.date).format("LLL");
      return `${date} : ${note.person}`;
    },
    validate() {
      if (this.comment.indexOf("??") !== -1) {
        alert("請編輯更新內容!");
        return false;
      } else return true;
    },
    save() {
      axios
        .put("/BuildCase", this.buildCase)
        .then(resp => {
          let ret = resp.data;
          if (ret.Ok) {
            alert("成功!");
          }
        })
        .catch(err => alert(err));
    },
    onFormChanged(evt) {
      this.$emit("Changed", this.buildCase._id);
    }
  },
  components: {
    Builder,
    BuildCaseForm
  }
};
</script>

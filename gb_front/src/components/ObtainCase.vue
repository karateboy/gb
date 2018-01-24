<template>
    <div>
        <div class="row">
            <div class="form-horizontal">
                <div class="form-group">
                    <label class="col-sm-2 control-label">名單:</label>
                    <div class="col-lg-10">
                    <div class="btn-group" data-toggle="buttons">
                        <label class="btn btn-outline btn-primary dim" 
                               v-for="(obj, idx) in dirList" :class="{active: dirIdx==idx}"
                               @click="dirIdx=idx">
                            <input type="radio">{{ obj.name }} </label>
                    </div>                    
                </div>
                </div>
                <div class="form-group">
                    <label class="col-sm-2 control-label">種類:</label>
                    <div class="col-lg-10">
                    <div class="btn-group" data-toggle="buttons">
                        <label class="btn btn-outline btn-primary dim" 
                               v-for="(obj, idx) in typeList" :class="{active: typeIdx==idx}"
                               @click="typeIdx=idx">
                            <input type="radio">{{ obj.name }} </label>
                    </div>                    
                </div>
              </div>
            </div>
        </div>
        <build-case2-list v-if="typeIdx===0" :url="targetUrl" :param="{}" :obtainBtn="true" :download="isAdmin"></build-case2-list>
        <care-house-list v-if="typeIdx===1" :url="targetUrl" :param="{}" :obtainBtn="true" :download="isAdmin"></care-house-list>
    </div>
</template>
<style>

</style>
<script>
import BuildCase2List from "./BuildCase2List.vue";
import CareHouseList from "./CareHouseList.vue";
import { mapGetters } from "vuex";
import axios from "axios";

export default {
  data() {
    return {
      dirIdx: 0,
      dirList: [
        {
          name: "北向",
          typeID: "N"
        },
        {
          name: "南向",
          typeID: "S"
        }
      ],
      typeIdx: 0,
      typeList: []
    };
  },
  mounted() {
    axios
      .get("/TargetWorkPointType")
      .then(resp => {
        const ret = resp.data;
        this.typeList.splice(0, this.typeList.length);
        for (let type of ret) {
          this.typeList.push(type);
        }
      })
      .catch(err => alert(err));
  },
  computed: {
    ...mapGetters(["isAdmin"]),
    targetUrl() {
      return `/Ownerless/${this.dirList[this.dirIdx].typeID}/${
        this.typeList[this.typeIdx].typeID
      }`;
    }
  },
  methods: {},
  components: {
    BuildCase2List,
    CareHouseList
  }
};
</script>

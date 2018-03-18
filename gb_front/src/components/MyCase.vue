<template>
    <div>
        <div class="row">
            <div class="form-horizontal">                
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
        <build-case2-list v-if="typeIdx===0" :url="targetUrl" :param="{}" :download="true"></build-case2-list>
        <care-house-list v-if="typeIdx===1" :url="targetUrl" :param="{}"  :download="true"></care-house-list>
    </div>
</template>
<style>
body {
}
</style>
<script>
import BuildCase2List from "./BuildCase2List.vue";
import CareHouseList from "./CareHouseList.vue";
import { mapGetters } from "vuex";
import axios from "axios";

export default {
  data() {
    return {
      typeIdx: 0,
      typeList: [
        {
          _id: 1,
          typeID: "BuildCase",
          name: "起造人"
        },
        {
          _id: 2,
          typeID: "CareHouse",
          name: "長照機構"
        }
      ]
    };
  },
  mounted() {},
  computed: {
    targetUrl() {
      return `/MyCase/${this.typeList[this.typeIdx].typeID}`;
    }
  },
  components: {
    BuildCase2List,
    CareHouseList
  }
};
</script>

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
                               :key="idx"
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
                               :key="idx"
                               @click="typeIdx=idx">
                            <input type="radio">{{ obj.name }} </label>
                    </div>                    
                </div>
              </div>
            </div>
        </div>
        <build-case2-list v-if="typeIdx===0" :url="targetUrl" :param="{}" :obtainBtn="obtainBtn" :download="true" :dm="isAdmin" :split-case="isAdmin"></build-case2-list>
        <care-house-list v-if="typeIdx===1" :url="targetUrl" :param="{}" :obtainBtn="obtainBtn" :download="true" :dm="isAdmin" :split-case="isAdmin"></care-house-list>
        <facility-list v-if="typeIdx===2" :url="targetUrl" :param="{}" :obtainBtn="obtainBtn" :download="true" :dm="isAdmin" :split-case="isAdmin"></facility-list>
    </div>
</template>
<style>

</style>
<script>
import BuildCase2List from "./BuildCase2List.vue";
import CareHouseList from "./CareHouseList.vue";
import FacilityList from "./FacilityList.vue";
import { mapGetters } from "vuex";
import axios from "axios";

export default {
  props: {
    url: {
      type: String,
      required: true
    },
    obtainBtn: {
      type: Boolean,
      default: false
    }
  },
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
        },
        {
          _id: 3,
          typeID: "Facility",
          name: "有槽工廠"
        }
      ]
    };
  },
  mounted() {},
  computed: {
    ...mapGetters(["isAdmin"]),
    targetUrl() {
      return `/${this.url}/${this.dirList[this.dirIdx].typeID}/${
        this.typeList[this.typeIdx].typeID
      }`;
    }
  },
  methods: {},
  components: {
    BuildCase2List,
    CareHouseList,
    FacilityList
  }
};
</script>

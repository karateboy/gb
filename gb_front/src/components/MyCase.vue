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
    </div>
</template>
<style>
body {
}
</style>
<script>
import BuildCase2List from "./BuildCase2List.vue";
import { mapGetters } from "vuex";
import axios from "axios";

export default {
  data() {
    return {
      typeIdx: 0,
      typeList: [
        {
          name: "起造人",
          url: "BuildCase"
        },
        {
          name: "長照機構",
          url: "CareHouse"
        }
      ]
    };
  },
  computed: {
    targetUrl() {
      return `/MyCase/${this.typeList[this.typeIdx].url}`;
    }
  },
  components: {
    BuildCase2List
  }
};
</script>

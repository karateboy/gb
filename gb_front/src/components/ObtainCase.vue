<template>
    <div>
        <div class="row">
            <div class="form-horizontal">
                <div class="form-group">
                    <label class="col-sm-2 control-label">名單:</label>
                    <div class="col-lg-10">
                    <div class="btn-group" data-toggle="buttons">
                        <label class="btn btn-outline btn-primary dim" 
                               v-for="(obj, idx) in urlList" :class="{active: urlIndex==idx}"
                               @click="urlIndex=idx">
                            <input type="radio">{{ obj.name }} </label>
                    </div>
                </div>
            </div>
            </div>
        </div>
        <build-case2-list :url="urlList[urlIndex].url" :param="{ sorby:'area+'}" :obtainBtn="true"></build-case2-list>
    </div>
</template>
<style>

</style>
<script>
import BuildCase2List from "./BuildCase2List.vue";
import { mapGetters } from "vuex";
import axios from "axios";

export default {
  data() {
    return {
      urlIndex: 0,
      urlList: [
        {
          name: "北向",
          url: "/NorthOwnerless"
        },
        {
          name: "南向",
          url: "/SouthOwnerless"
        }
      ]
    };
  },
  components: {
    BuildCase2List
  }
};
</script>

<template>
    <div>
        <br>
        <div class="form-horizontal">
            <div class="form-group">
                <label class="col-lg-2 control-label">下載種類:</label>
                <div class="col-lg-7">
                    <div class="btn-group" data-toggle="buttons">
                        <label class="btn btn-outline btn-primary dim"
                               v-for="download in downloadTypeList"
                               @click="dlType=download.id"
                               :key = "download.id"
                               >
                            <input type="radio">{{ download.desc }} </label>
                    </div>
                </div>
            </div>
            <div class="form-group">
                <div class="col-lg-offset-1 col-lg-4">
                    <button class="btn btn-primary" @click="download()">下載DM信封</button>
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
import baseUrl from "../baseUrl";

export default {
  data() {
    return {
      downloadTypeList: [],
      dlType: undefined
    };
  },
  mounted() {
    axios
      .get("/DownloadType")
      .then(resp => {
        const ret = resp.data;
        this.downloadTypeList.splice(0, this.downloadTypeList.length);
        for (let type of ret) {
          this.downloadTypeList.push(type);
        }
      })
      .catch(err => alert(err));
  },
  computed: {
    targetUrl() {
      return `/Download/${this.dlType.id}`;
    }
  },
  methods: {
    download() {
      let url = baseUrl() + `/Download/${this.dlType}`;
      window.open(url);
    }
  },
  components: {}
};
</script>

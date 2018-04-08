<template>
    <div>
        <br>
        <div class="panel panel-success">
            <div class="panel-heading">
                機構訪查表
            </div>
            <div class="panel-body">
              <table class="table">
                <thead>
                  <tr>
                    <th>項目</th>
                    <th>紀錄</th>
                  </tr>
                </thead>
                    <tbody>
                        <tr><th>尿布配送</th>
                          <td><input type="text" class="form-control" v-model="form.diaper"></td>
                        </tr>
                        <tr><th>魚豬肉用量</th>
                          <td><input type="text" class="form-control" v-model="form.food"></td>
                        </tr>
                        <tr><th>皮膚用保養品</th>
                          <td><input type="text" class="form-control" v-model="form.skin"></td>
                        </tr>
                        <tr><th>水素水</th>
                          <td><input type="text" class="form-control" v-model="form.water"></td>
                        </tr>
                        <tr><th>機構照片</th>
                          <td>
                            <img v-if="form.photos[0] != '000000000000000000000000'" :src="imageUrl(form.photos[0])" alt="機構照片" class="img-thumbnail" width="400">
                            <form enctype="multipart/form-data" novalidate>
                                <input type="file" name="0" multiple @change="filesChange($event.target.name, $event.target.files)" accept="image/*" class="btn btn-primary">
                            </form>
                          </td>
                        </tr>
                        <tr><th>護士照片</th>
                          <td>
                            <img v-if="form.photos[1] != '000000000000000000000000'" :src="imageUrl(form.photos[1])" alt="護士照片" class="img-thumbnail" width="400">
                            <form enctype="multipart/form-data" novalidate>
                                <input type="file" name="1" multiple @change="filesChange($event.target.name, $event.target.files)" accept="image/*" class="btn btn-primary">
                            </form>
                          </td>
                        </tr>
                        <tr><th>醫生照片</th>
                          <td>
                            <img v-if="form.photos[2] != '000000000000000000000000'" :src="imageUrl(form.photos[2])" alt="醫生照片" class="img-thumbnail" width="400">
                            <form enctype="multipart/form-data" novalidate>
                                <input type="file" name="2" multiple @change="filesChange($event.target.name, $event.target.files)" accept="image/*" class="btn btn-primary">
                            </form>
                          </td>
                        </tr>
                        <tr><th>洗衣廠照片</th>
                          <td>
                            <img v-if="form.photos[3] != '000000000000000000000000'" :src="imageUrl(form.photos[3])" alt="洗衣廠照片" class="img-thumbnail" width="400">
                            <form enctype="multipart/form-data" novalidate>
                                <input type="file" name="3" multiple @change="filesChange($event.target.name, $event.target.files)" accept="image/*" class="btn btn-primary">
                            </form>
                          </td>
                        </tr>
                    </tbody>
              </table>              
              <div class="col-sm-1 col-sm-offset-1">
                    <button class='btn btn-primary' @click='save'>上傳</button>
              </div>
            </div>
        </div>
    </div>
</template>
<style scoped>

</style>
<script>
import Vue from "vue";
import axios from "axios";
import moment from "moment";
import { mapGetters } from "vuex";
import baseUrl from "../baseUrl";

export default {
  props: {
    careHouseID: {
      type: Object,
      required: true
    },
    careHouseForm: {
      type: Object,
      default: () => {
        const noPhotoID = "000000000000000000000000";
        return {
          diaper: "",
          food: "",
          skin: "",
          water: "",
          photos: [noPhotoID, noPhotoID, noPhotoID, noPhotoID],
          submitDate: new Date()
        };
      }
    }
  },
  watch: {
    careHouseForm: function(newForm) {
      this.form = Vue.util.extend({}, newForm);
    }
  },
  data() {
    return {
      form: Vue.util.extend({}, this.careHouseForm)
    };
  },
  computed: {
    ...mapGetters(["user"])
  },
  methods: {
    save() {
      let idJson = JSON.stringify(this.careHouseID);
      let url = `/CareHouseForm/${encodeURIComponent(idJson)}`;

      axios
        .post(url, this.form)
        .then(resp => {
          let ret = resp.data;
          if (ret.Ok) {
            alert("成功上傳!");
            this.$emit("formChanged", this.form)
          }
        })
        .catch(err => alert(err));
    },
    upload(formData) {
      const url = `${baseUrl()}/UploadPhoto`;
      return axios
        .post(url, formData)
        .then(resp => {
          const ret = resp.data;
          for (var pair of formData.entries()) {
            let idx = parseInt(pair[0]);
            this.$set(this.form.photos, idx, ret[0]);
          }
          alert("上傳成功");
        })
        .catch(err => alert(err));
    },
    filesChange(fieldName, fileList) {
      // handle file changes
      const formData = new FormData();

      if (!fileList.length) return;

      // append the files to FormData
      Array.from(Array(fileList.length).keys()).map(x => {
        formData.append(fieldName, fileList[x], fileList[x].name);
      });

      this.upload(formData);
    },
    imageUrl(id) {
      let url = baseUrl() + `/Photo/${id}`;
      return url;
    }
  },
  components: {}
};
</script>

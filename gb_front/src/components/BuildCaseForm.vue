<template>
    <div>
        <br>
        <div class="panel panel-success">
            <div class="panel-heading">
                建案訪查表
            </div>
            <div class="panel-body">
              <table class="table">
                <thead>
                  <tr>
                    <th>項目</th>
                    <th>窗口</th>
                    <th>地址</th>
                    <th>電話</th>
                  </tr>
                </thead>
                    <tbody>
                        <tr><th>營造廠</th>
                          <td><input type="text" class="form-control" v-model="form.constructor.name"></td>
                          <td><input type="text" class="form-control" v-model="form.constructor.addr"></td>
                          <td><input type="text" class="form-control" v-model="form.constructor.phone"></td>
                        </tr>
                        <tr><th>跑照人</th>
                          <td><input type="text" class="form-control" v-model="form.permit.name"></td>
                          <td><input type="text" class="form-control" v-model="form.permit.addr"></td>
                          <td><input type="text" class="form-control" v-model="form.permit.phone"></td>
                        </tr>
                        <tr><th>土方開挖</th>
                          <td><input type="text" class="form-control" v-model="form.earthWork.name"></td>
                          <td><input type="text" class="form-control" v-model="form.earthWork.addr"></td>
                          <td><input type="text" class="form-control" v-model="form.earthWork.phone"></td>
                        </tr>
                        <tr><th>營建廢棄物</th>
                          <td><input type="text" class="form-control" v-model="form.dump.name"></td>
                          <td><input type="text" class="form-control" v-model="form.dump.addr"></td>
                          <td><input type="text" class="form-control" v-model="form.dump.phone"></td>
                        </tr>
                        <tr><th>焚化垃圾</th>
                          <td><input type="text" class="form-control" v-model="form.burner.name"></td>
                          <td><input type="text" class="form-control" v-model="form.burner.addr"></td>
                          <td><input type="text" class="form-control" v-model="form.burner.phone"></td>
                        </tr>
                        <tr><th>全磚牆泥做</th>
                          <td><input type="text" class="form-control" v-model="form.wall.name"></td>
                          <td><input type="text" class="form-control" v-model="form.wall.addr"></td>
                          <td><input type="text" class="form-control" v-model="form.wall.phone"></td>
                        </tr> 
                        <tr><th>備註</th>
                          <td colspan="3"><textarea class="form-control" rows="4" v-model="form.comment"></textarea></td>
                        </tr>                                               
                        <tr><th>工地辦公室照片</th>
                          <td colspan="3">
                            <img v-if="form.photos[0] != '000000000000000000000000'" :src="imageUrl(form.photos[0])" alt="工地辦公室" class="img-thumbnail" width="400">
                            <form enctype="multipart/form-data" novalidate>
                                <input type="file" name="0" multiple @change="filesChange($event.target.name, $event.target.files)" accept="image/*" class="btn btn-primary">
                            </form>
                          </td>
                        </tr>
                        <tr><th>工地圍籬告示牌</th>
                          <td colspan="3">
                            <img v-if="form.photos[1] != '000000000000000000000000'" :src="imageUrl(form.photos[1])" alt="工地圍籬告示牌" class="img-thumbnail" width="400">
                            <form enctype="multipart/form-data" novalidate>
                                <input type="file" name="1" multiple @change="filesChange($event.target.name, $event.target.files)" accept="image/*" class="btn btn-primary">
                            </form>
                          </td>
                        </tr>
                        <tr><th>工地面前道路</th>
                          <td colspan="3">
                            <img v-if="form.photos[2] != '000000000000000000000000'" :src="imageUrl(form.photos[2])" alt="工地面前道路" class="img-thumbnail" width="400">
                            <form enctype="multipart/form-data" novalidate>
                                <input type="file" name="2" multiple @change="filesChange($event.target.name, $event.target.files)" accept="image/*" class="btn btn-primary">
                            </form>
                          </td>
                        </tr>
                        <tr><th>建築師事務所門口</th>
                          <td colspan="3">
                            <img v-if="form.photos[3] != '000000000000000000000000'" :src="imageUrl(form.photos[3])" alt="建築師事務所門口" class="img-thumbnail" width="400">
                            <form enctype="multipart/form-data" novalidate>
                                <input type="file" name="3" multiple @change="filesChange($event.target.name, $event.target.files)" accept="image/*" class="btn btn-primary">
                            </form>
                          </td>
                        </tr>
                    </tbody>
              </table>              
              <div class="col-sm-1 col-sm-offset-1">
                    <button class='btn btn-primary' @click='save'>上傳透明度表格</button>
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
    buildCaseID: {
      type: Object,
      required: true
    },
    buildCaseForm: {
      type: Object,
      default: () => {
        const noPhotoID = "000000000000000000000000";
        return {
          constructor: {
            name: "",
            addr: "",
            phone: ""
          },
          permit: {
            name: "",
            addr: "",
            phone: ""
          },
          earthWork: {
            name: "",
            addr: "",
            phone: ""
          },
          dump: {
            name: "",
            addr: "",
            phone: ""
          },
          burner: {
            name: "",
            addr: "",
            phone: ""
          },
          wall: {
            name: "",
            addr: "",
            phone: ""
          },
          comment: "",
          photos: [noPhotoID, noPhotoID, noPhotoID, noPhotoID],
          submitDate: new Date()
        };
      }
    }
  },
  watch: {
    buildCaseForm: function(newForm) {
      this.form = Vue.util.extend({}, newForm)
    }
  },
  data() {
    return {
      form: Vue.util.extend({}, this.buildCaseForm)
    };
  },
  computed: {
    ...mapGetters(["user"])
  },
  methods: {
    save() {
      console.log(this.form);
      let idJson = JSON.stringify(this.buildCaseID);
      let url = `/BuildCaseForm/${encodeURIComponent(idJson)}`;

      axios
        .post(url, this.form)
        .then(resp => {
          let ret = resp.data;
          if (ret.Ok) {
            alert("成功!");
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

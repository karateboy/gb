<template>
    <div>
        <br>
        <div class="form-horizontal">
            <div class="form-group">
                <label class="col-sm-1 control-label">縣市:</label>
                <div class="col-sm-4"><input type="text" class="form-control" :value="careHouse._id.county"></div>
            </div>
            <div class="form-group">
                <label class="col-sm-1 control-label">名稱:</label>
                <div class="col-sm-4"><input type="text" class="form-control" :value="careHouse._id.name"></div>
            </div>
            <div class="form-group">
                <label class="col-sm-1 control-label">電話:</label>
                <div class="col-sm-4">
                  <a :href="'tel:' + careHouse.phone">{{careHouse.phone}}</a>
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-1 control-label">傳真:</label>
                <div class="col-sm-4"><input type="text" class="form-control" :value="careHouse.fax"></div>
            </div>
            <div class="form-group">
                <label class="col-sm-1 control-label">email:</label>
                <div class="col-sm-4"><input type="text" class="form-control" :value="careHouse.email"></div>
            </div>
            <div class="form-group">
                <label class="col-sm-1 control-label">業務:</label>
                <div class="col-sm-4"><input type="text" class="form-control" :value="careHouse.owner" readonly></div>
            </div>
            <care-house-form :careHouseID="careHouse._id" :careHouseForm="careHouse.form" @formChanged="onFormChanged"></care-house-form>
        </div>
    </div>
</template>
<style scoped>

</style>
<script>
import axios from "axios";
import moment from "moment";
import { mapGetters } from "vuex";
import CareHouseForm from "./CareHouseForm.vue";

export default {
  props: {
    careHouse: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      comment: "已電話聯絡??, 已約見面??, 遇到困難??"
    };
  },
  mounted() {},
  computed: {
    ...mapGetters(["user"])
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
      if (!this.validate()) return;

      let newNote = {
        date: new Date(),
        comment: this.comment,
        person: this.user.name
      };

      this.careHouse.notes.push(newNote);

      axios
        .put("/CareHouse", this.careHouse)
        .then(resp => {
          let ret = resp.data;
          if (ret.Ok) {
            alert("成功!");
          }
        })
        .catch(err => alert(err));
    },
    onFormChanged(evt){
      this.$emit("Changed", this.careHouse._id)
    }
  },
  components: {
    CareHouseForm
  }
};
</script>

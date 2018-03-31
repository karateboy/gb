<template>
    <div>
        <br>
        <div class="form-horizontal">
            <div class="form-group">
                <label class="col-sm-1 control-label">管制編號:</label>
                <div class="col-sm-4"><input type="text" class="form-control" v-model="facility._id"></div>
            </div>
            <div class="form-group">
                <label class="col-sm-1 control-label">名稱:</label>
                <div class="col-sm-4"><input type="text" class="form-control" v-model="facility.name"></div>
            </div>
            <div class="form-group">
                <label class="col-sm-1 control-label">電話:</label>
                <div class="col-sm-4">
                  <a :href="'tel:' + facility.phone">{{facility.phone}}</a>
                  <input type="text" class="form-control" v-model="facility.phone"></div>
            </div>
            <div class="form-group">
                <label class="col-sm-1 control-label">業務:</label>
                <div class="col-sm-4"><input type="text" class="form-control" :value="facility.owner" readonly></div>
            </div>
            <entry-item id="entryModal" :ioType='ioType' :opType='opType' :entryIndex='entryIndex'
                                   :entry='getEntry()'
                                   @addEntry='addEntry'
                                   @updateEntry='updateEntry'
                ></entry-item>
            <div class="form-group">
                <label class="col-sm-1 control-label">處理代碼:</label>
                    <div class="col-sm-4">
                        <table class="table table-bordered">
                            <thead>
                            <tr>
                                <th>名稱</th>
                                <th>代碼</th>
                                <th>數量</th>
                            </tr>
                            </thead>
                            <tbody>
                            <tr v-for="(wi, idx) in facility.wasteIn" :key="idx">
                                <td>{{wi.wasteName}}</td>
                                <td>{{wi.wasteCode}}</td>
                                <td>{{wi.totalQuantity}}</td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
            </div>
            <div class="form-group">
                <label class="col-sm-1 control-label">產出:</label>
                    <div class="col-sm-4">
                        <table class="table table-bordered">
                            <thead>
                            <tr>
                                <th>日期</th>
                                <th>代碼</th>
                                <th>頻率</th>
                                <th>數量</th>
                            </tr>
                            </thead>
                            <tbody>
                            <tr v-for="(wo, idx) in facility.wasteOut" :key="idx">
                                <td>{{wo.date}}</td>
                                <td>{{wo.wasteCode}}</td>
                                <td>{{wo.wasteName}}</td>
                                <td>{{wo.quantity}}</td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
            </div>
            <div class="panel panel-success" v-for="(note, idx) in facility.notes" :key="idx">
                <div class="panel-heading">{{noteHeader(note)}}</div>
                <div class="panel-body">
                    {{note.comment}}
                </div>
            </div>

            <div class="form-group">
                <label class="col-sm-1 control-label" for="comment">更新內容:</label>
                <div class="col-sm-4"> <textarea class="form-control" rows="3" id="comment" v-model="comment"></textarea></div>
            </div>
            <div class="form-group">
                <div class="col-sm-1 col-sm-offset-1">
                    <button class='btn btn-primary' @click='save'>更新</button>
                </div>
            </div>
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
import EntryItem from "./EntryItem.vue";

export default {
  props: {
    facility: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      comment: "已電話聯絡??, 已約見面??, 遇到困難??",
      displayBuilder: false,
      builder: {},
      ioType: "in",
      opType: "add",
      entryIndex: 0,
      entry: {}
    };
  },
  mounted() {

  },
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

      this.facility.notes.push(newNote);

      axios
        .put("/Facility", this.facility)
        .then(resp => {
          let ret = resp.data;
          if (ret.Ok) {
            alert("成功!");
          }
        })
        .catch(err => alert(err));
    },
    getEntry() {
      if (this.opType === "add") return this.entry;
      else {
        if (this.ioType === "in" && this.facility.in.length != 0)
          return this.facility.in[this.entryIndex];
        else if (this.ioType === "out" && this.facility.out.length != 0)
          return this.facility.out[this.entryIndex];
        else return this.entry;
      }
    },
    addEntry(evt) {
      var copy = Object.assign({}, evt.entry);
      if (evt.ioType === "in") this.facility.in.push(copy);
      else this.facility.out.push(copy);
    },
    delEntry(ioType, idx) {
      if (ioType === "in") this.facility.in.splice(idx, 1);
      else this.facility.out.splice(idx, 1);
    },
    editEntry(ioType, idx) {
      this.ioType = ioType;
      this.opType = "edit";
      this.entryIndex = idx;
    },
    updateEntry(evt) {
      if (this.ioType === "in") {
        this.facility.in[this.entryIndex] = evt.entry;
      } else {
        this.facility.out[this.entryIndex] = evt.entry;
      }
    }
  },
  components: {
    EntryItem
  }
};
</script>

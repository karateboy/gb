<template>
    <div>
        <br>
        <div class="form-horizontal">
            <div class="form-group">
                <label class="col-sm-1 control-label">縣市:</label>
                <div class="col-sm-4"><input type="text" class="form-control" v-model="careHouse._id.county"></div>
            </div>
            <div class="form-group">
                <label class="col-sm-1 control-label">名稱:</label>
                <div class="col-sm-4"><input type="text" class="form-control" v-model="careHouse._id.name"></div>
            </div>
            <div class="form-group">
                <label class="col-sm-1 control-label">電話:</label>
                <div class="col-sm-4">
                  <a :href="'tel:' + careHouse.phone">{{careHouse.phone}}</a>
                  <input type="text" class="form-control" v-model="careHouse.phone"></div>
            </div>
            <div class="form-group">
                <label class="col-sm-1 control-label">傳真:</label>
                <div class="col-sm-4"><input type="text" class="form-control" v-model="careHouse.fax"></div>
            </div>
            <div class="form-group">
                <label class="col-sm-1 control-label">email:</label>
                <div class="col-sm-4"><input type="text" class="form-control" v-model="careHouse.email"></div>
            </div>
            <div class="form-group">
                <label class="col-sm-1 control-label">業務:</label>
                <div class="col-sm-4"><input type="text" class="form-control" :value="careHouse.owner" readonly></div>
            </div>
            <entry-item id="entryModal" :ioType='ioType' :opType='opType' :entryIndex='entryIndex'
                                   :entry='getEntry()'
                                   @addEntry='addEntry'
                                   @updateEntry='updateEntry'
                ></entry-item>
            <div class="form-group">
                <label class="col-sm-1 control-label">輸入:</label>
                    <div class="col-sm-4">
                        <table class="table table-bordered">
                            <thead>
                            <tr>
                                <th>名稱</th>
                                <th>代碼</th>
                                <th>頻率</th>
                                <th>數量</th>
                                <th></th>
                            </tr>
                            </thead>
                            <tbody>
                            <tr v-for="(input, idx) in careHouse.in">
                                <td>{{input.name}}</td>
                                <td>{{input.code}}</td>
                                <td>{{input.freq}}</td>
                                <td>{{input.volume}}</td>
                                <td>
                                    <button class="btn btn-danger" @click="delEntry('in',idx)">
                                        <i class="fa fa-trash" aria-hidden="true"></i>&nbsp;刪除
                                    </button>
                                    <button class="btn btn-warning" @click="editEntry('in',idx)" data-toggle="modal" data-target="#entryModal">
                                        <i class="fa fa-pencil" aria-hidden="true"></i>&nbsp;更新
                                    </button>
                                </td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                    <button type="button" class="btn btn-primary" data-toggle="modal" data-target="#entryModal" @click="opType='add';ioType='in'">
                        <i class="fa fa-plus" aria-hidden="true"></i>&nbsp;新增
                    </button>
            </div>
            <div class="form-group">
                <label class="col-sm-1 control-label">產出:</label>
                    <div class="col-sm-4">
                        <table class="table table-bordered">
                            <thead>
                            <tr>
                                <th>名稱</th>
                                <th>代碼</th>
                                <th>頻率</th>
                                <th>數量</th>
                                <th></th>
                            </tr>
                            </thead>
                            <tbody>
                            <tr v-for="(input, idx) in careHouse.out">
                                <td>{{input.name}}</td>
                                <td>{{input.code}}</td>
                                <td>{{input.freq}}</td>
                                <td>{{input.volume}}</td>
                                <td>
                                    <button class="btn btn-danger" @click="delEntry('out',idx)">
                                        <i class="fa fa-trash" aria-hidden="true"></i>&nbsp;刪除
                                    </button>
                                    <button class="btn btn-warning" @click="editEntry('out',idx)" data-toggle="modal" data-target="#entryModal">
                                        <i class="fa fa-pencil" aria-hidden="true"></i>&nbsp;更新
                                    </button>
                                </td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                    <button type="button" class="btn btn-primary" data-toggle="modal" data-target="#entryModal" @click="opType='add';ioType='out'">
                        <i class="fa fa-plus" aria-hidden="true"></i>&nbsp;新增
                    </button>
            </div>
            <div class="panel panel-success" v-for="(note, idx) in careHouse.notes">
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
    careHouse: {
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
    getEntry() {
      if (this.opType === "add") return this.entry;
      else {
        if (this.ioType === "in" && this.careHouse.in.length != 0)
          return this.careHouse.in[this.entryIndex];
        else if (this.ioType === "out" && this.careHouse.out.length != 0)
          return this.careHouse.out[this.entryIndex];
        else return this.entry;
      }
    },
    addEntry(evt) {
      var copy = Object.assign({}, evt.entry);
      if (evt.ioType === "in") this.careHouse.in.push(copy);
      else this.careHouse.out.push(copy);
    },
    delEntry(ioType, idx) {
      if (ioType === "in") this.careHouse.in.splice(idx, 1);
      else this.careHouse.out.splice(idx, 1);
    },
    editEntry(ioType, idx) {
      this.ioType = ioType;
      this.opType = "edit";
      this.entryIndex = idx;
    },
    updateEntry(evt) {
      if (this.ioType === "in") {
        this.careHouse.in[this.entryIndex] = evt.entry;
      } else {
        this.careHouse.out[this.entryIndex] = evt.entry;
      }
    }
  },
  components: {
    EntryItem
  }
};
</script>

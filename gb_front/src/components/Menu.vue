<template>
    <div class="sidebar-collapse">
        <ul class="nav metismenu" id="side-menu">
            <li class="nav-header">
                <div class="dropdown profile-element">
                    <a data-toggle="dropdown" class="dropdown-toggle" href="#">
                        <span class="clear">
                            <span class="block m-t-xs">
                                <strong class="font-bold">{{ user.name }}</strong>
                            </span>
                            <span class="text-muted text-xs block">{{ groupInfoMap[user.groupId]}}
                                <b class="caret"></b>
                            </span>
                        </span>
                    </a>
                    <ul class="dropdown-menu animated fadeInRight m-t-xs">
                        <li>
                            <a href="#">Logout</a>
                        </li>
                    </ul>
                </div>
                <div class="logo-element">
                    IN+
                </div>
            </li>
            <router-link v-show="false" tag="li" to="/" active-class="active" exact>
                <a>
                    <i class="fa fa-tachometer" aria-hidden="true"></i>
                    <span class="nav-label">儀錶板</span>
                </a>
            </router-link>
            <li v-show="isSales||isAdmin">
                <a>
                    <i class="fa fa-user-circle-o" aria-hidden="true"></i>
                    <span class="nav-label">業務</span>
                    <span class="fa arrow"></span>
                </a>
                <ul class="nav nav-second-level collapse">
                    <router-link tag="li" :to="{name:'MyCase'}" active-class="active">
                        <a>
                            <i class="fa fa-diamond" aria-hidden="true"></i>
                            <span class="nav-label"></span>我的名單</a>
                    </router-link>
                    <router-link tag="li" :to="{name:'ObtainCase'}" active-class="active">
                        <a>
                            <i class="fa fa-search" aria-hidden="true"></i>
                            <span class="nav-label"></span>取得名單</a>
                    </router-link>
                    <router-link tag="li" :to="{name:'UsageReport'}" active-class="active">
                        <a>
                            <i class="fa fa-book" aria-hidden="true"></i>
                            <span class="nav-label"></span>使用報告</a>
                    </router-link>
                    <router-link tag="li" :to="{name:'Map'}" active-class="active">
                        <a>
                            <i class="fa fa-globe" aria-hidden="true"></i>
                            <span class="nav-label"></span>業務地圖</a>
                    </router-link>
                </ul>
            </li>
            <li v-show="isIntern">
                <a>
                    <i class="fa fa-users" aria-hidden="true"></i>
                    <span class="nav-label">工讀生</span>
                    <span class="fa arrow"></span>
                </a>
                <ul class="nav nav-second-level collapse">
                    <router-link to='/Intern/Builder' tag='li' role="presentation" active-class='active'><a>起造人電話</a>
                    </router-link>
                    <router-link to='/Intern/BuildCase' tag='li' role="presentation" active-class='active'><a>建案資訊</a>
                    </router-link>
                    <router-link to='/Intern/Contractor' tag='li' role="presentation" active-class='active'><a>承造人</a>
                    </router-link>
                    <router-link to='/Intern/Report' tag='li' role="presentation" active-class='active'><a>工作報告</a>
                    </router-link>
                </ul>
            </li>
            <li v-show="isAdmin">
                <a>
                    <i class="fa fa-cog" aria-hidden="true"></i>
                    <span class="nav-label">系統管理</span>
                    <span class="fa arrow"></span>
                </a>
                <ul class="nav nav-second-level collapse">
                    <router-link tag="li" :to="{name:'AddUser'}" active-class="active">
                        <a>
                            <i class="fa fa-plus" aria-hidden="true"></i>
                            <span class="nav-label"></span>新增使用者</a>
                    </router-link>

                    <router-link tag="li" :to="{name:'DelUser'}" active-class="active">
                        <a>
                            <i class="fa fa-trash" aria-hidden="true"></i>
                            <span class="nav-label"></span>刪除使用者</a>
                    </router-link>

                    <router-link tag="li" :to="{name:'UpdateUser'}" active-class="active">
                        <a>
                            <i class="fa fa-pencil" aria-hidden="true"></i>
                            <span class="nav-label"></span>更新使用者</a>
                    </router-link>
                </ul>
            </li>
        </ul>
    </div>
</template>
<style scoped>

</style>
<script>
import { mapGetters } from "vuex";
import axios from "axios";
export default {
  data() {
    axios.get("/Group").then(resp => {
      const ret = resp.data;
      for (let groupInfo of ret) {
        this.groupInfoMap[groupInfo.id] = groupInfo.name;
      }
    });
    return {
      groupInfoMap: {}
    };
  },
  computed: {
    ...mapGetters(["user", "isAdmin", "isSales", "isIntern"])
  }
};
</script>
